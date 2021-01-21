/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.air;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Burstable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AirBlast extends AbilityInstance implements Ability, Burstable {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private AirStream stream;
	private Vector3 origin;
	private Vector3 direction;

	private boolean launched;
	private boolean selectedOrigin;
	private int particleCount;
	private long renderInterval;

	public AirBlast(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		particleCount = 6;

		if (Policies.IN_LIQUID.test(user, getDescription())) {
			return false;
		}

		removalPolicy = Policies.builder()
			.add(new OutOfRangeRemovalPolicy(userConfig.selectRange * 2, () -> origin))
			.add(Policies.IN_LIQUID)
			.build();

		for (AirBlast blast : Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, AirBlast.class).collect(Collectors.toList())) {
			if (!blast.launched) {
				if (method == ActivationMethod.SNEAK_RELEASE) {
					if (!blast.selectOrigin()) {
						Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(blast);
					}
				} else {
					blast.launch();
				}
				return false;
			}
		}

		if (method == ActivationMethod.SNEAK_RELEASE) {
			return selectOrigin();
		} else {
			origin = user.getEyeLocation();
			launch();
		}
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}

		if (!launched) {
			if (!getDescription().equals(user.getSelectedAbility().orElse(null))) {
				return UpdateResult.REMOVE;
			}
			ParticleUtil.createAir(origin.toLocation(user.getWorld())).count(4).offset(0.5, 0.5, 0.5).spawn();
		}

		return (!launched || stream.update() == UpdateResult.CONTINUE) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	private boolean selectOrigin() {
		origin = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.selectRange))
			.subtract(user.getDirection().scalarMultiply(0.5));
		selectedOrigin = true;
		return Bending.getGame().getProtectionSystem().canBuild(user, origin.toBlock(user.getWorld()));
	}

	private void launch() {
		launched = true;
		Vector3 target = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range));
		if (user.isSneaking()) {
			Vector3 temp = new Vector3(origin.toArray());
			origin = new Vector3(target.toArray());
			target = temp;
		}
		direction = target.subtract(origin).normalize();
		user.setCooldown(getDescription(), userConfig.cooldown);
		stream = new AirStream(user, new Ray(origin, direction.scalarMultiply(userConfig.range)));
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		if (stream == null) return Collections.emptyList();
		return Collections.singletonList(stream.getCollider());
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	// Used to initialize the blast for bursts.
	@Override
	public void initialize(@NonNull User user, @NonNull Vector3 location, @NonNull Vector3 direction) {
		this.user = user;
		recalculateConfig();
		selectedOrigin = false;
		launched = true;
		origin = location;
		this.direction = direction;
		removalPolicy = Policies.builder().build();
		stream = new AirStream(user, new Ray(location, direction));
	}

	@Override
	public void setRenderInterval(long interval) {
		this.renderInterval = interval;
	}

	@Override
	public void setRenderParticleCount(int count) {
		this.particleCount = count;
	}

	private class AirStream extends ParticleStream {
		private long nextRenderTime;

		public AirStream(User user, Ray ray) {
			super(user, ray, userConfig.speed, 1.3);
			canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
		}

		@Override
		public void render() {
			long time = System.currentTimeMillis();
			if (time > nextRenderTime) {
				ParticleUtil.createAir(getBukkitLocation()).count(particleCount)
					.offset(0.275, 0.275, 0.275)
					.spawn();
				nextRenderTime = time + renderInterval;
			}
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.AIR_SOUND.play(getBukkitLocation());
			}

			// Handle user separately from the general entity collision.
			if (selectedOrigin) {
				if (AABBUtils.getEntityBounds(user.getEntity()).intersects(collider)) {
					onEntityHit(user.getEntity());
				}
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			double factor = entity.equals(user.getEntity()) ? userConfig.selfPush : userConfig.otherPush;
			factor *= 1.0 - (location.distance(origin) / (2 * userConfig.range));
			// Reduce the push if the player is on the ground.
			if (!WorldMethods.isOnGround(entity)) {
				factor *= userConfig.airborneFactor;
			}
			Vector3 velocity = new Vector3(entity.getVelocity());
			// The strength of the entity's velocity in the direction of the blast.
			double strength = velocity.dotProduct(direction);
			if (strength > factor) {
				velocity = velocity.scalarMultiply(0.5).add(direction.scalarMultiply(strength / 2));
			} else if (strength > factor * 0.5) {
				velocity = velocity.add(direction.scalarMultiply(factor - strength));
			} else {
				velocity = velocity.add(direction.scalarMultiply(factor * 0.5));
			}
			entity.setVelocity(velocity.clampVelocity());
			entity.setFireTicks(0);
			return false;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			if (BlockMethods.extinguishFire(user, block)) return false;
			BlockMethods.coolLava(user, block);
			return true;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.SPEED)
		public double speed;
		@Attribute(Attribute.STRENGTH)
		public double selfPush;
		@Attribute(Attribute.STRENGTH)
		public double otherPush;
		@Attribute(Attribute.STRENGTH)
		public double airborneFactor;
		@Attribute(Attribute.SELECTION)
		public double selectRange;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airblast");

			cooldown = abilityNode.node("cooldown").getLong(1500);
			range = abilityNode.node("range").getDouble(25.0);
			speed = abilityNode.node("speed").getDouble(1.25);

			selfPush = abilityNode.node("push").node("self").getDouble(2.2);
			otherPush = abilityNode.node("push").node("other").getDouble(2.2);
			airborneFactor = abilityNode.node("push").node("airborne").getDouble(0.5);

			selectRange = abilityNode.node("select-range").getDouble(10.0);
		}
	}
}
