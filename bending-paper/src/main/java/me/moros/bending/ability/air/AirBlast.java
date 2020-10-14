/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.Burstable;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AirBlast implements Ability, Burstable {
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

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		particleCount = 6;

		if (Policies.IN_LIQUID.test(user, getDescription()) || !Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}

		removalPolicy = Policies.builder()
			.add(new OutOfRangeRemovalPolicy(userConfig.selectOutOfRange, () -> origin))
			.add(Policies.IN_LIQUID)
			.build();

		for (AirBlast blast : Game.getAbilityManager(user.getWorld()).getUserInstances(user, AirBlast.class).collect(Collectors.toList())) {
			if (!blast.launched) {
				if (method == ActivationMethod.SNEAK_RELEASE) {
					blast.selectOrigin();
					if (!Game.getProtectionSystem().canBuild(user, blast.origin.toBlock(user.getWorld()))) {
						Game.getAbilityManager(user.getWorld()).destroyInstance(user, blast);
					}
				} else {
					blast.launch();
				}
				return false;
			}
		}

		if (method == ActivationMethod.SNEAK_RELEASE) {
			selectOrigin();
			return Game.getProtectionSystem().canBuild(user, origin.toBlock(user.getWorld()));
		} else {
			if (!Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
				return false;
			}
			origin = user.getEyeLocation();
			launch();
		}
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
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

	@Override
	public void onDestroy() {
	}

	private void selectOrigin() {
		origin = new Vector3(WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.selectRange)))
			.subtract(user.getDirection().scalarMultiply(0.5));
		selectedOrigin = true;
	}

	private boolean launch() {
		launched = true;
		Vector3 target = new Vector3(WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range)));
		if (user.isSneaking()) {
			Vector3 temp = new Vector3(origin.toArray());
			origin = new Vector3(target.toArray());
			target = temp;
		}
		direction = target.subtract(origin).normalize();
		user.setCooldown(getDescription(), userConfig.cooldown);
		stream = new AirStream(user, new Ray(origin, direction.scalarMultiply(userConfig.range)), userConfig.collisionRadius);
		return true;
	}

	@Override
	public Collection<Collider> getColliders() {
		if (stream == null) return Collections.emptyList();
		return Collections.singletonList(stream.getCollider());
	}

	@Override
	public void onCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "AirBlast";
	}

	// Used to initialize the blast for bursts.
	@Override
	public void initialize(User user, Vector3 location, Vector3 direction) {
		this.user = user;
		recalculateConfig();
		selectedOrigin = false;
		launched = true;
		origin = location;
		this.direction = direction;
		removalPolicy = Policies.builder().build();
		stream = new AirStream(user, new Ray(location, direction), userConfig.collisionRadius);
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

		public AirStream(User user, Ray ray, double collisionRadius) {
			super(user, ray, userConfig.speed, collisionRadius);
			canCollide = Block::isLiquid;
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
		public boolean onEntityHit(Entity entity) {
			double factor = entity.equals(user.getEntity()) ? userConfig.selfPush : userConfig.otherPush;
			factor *= 1.0 - (location.distance(origin) / (2 * userConfig.range));
			// Reduce the push if the player is on the ground.
			if (entity.equals(user.getEntity()) && WorldMethods.isOnGround(entity)) {
				factor *= 0.5;
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
			entity.setVelocity(velocity.clampVelocity().toVector());
			entity.setFireTicks(0);
			return false;
		}

		@Override
		public boolean onBlockHit(Block block) {
			return BlockMethods.extinguish(user, getBukkitLocation());
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.COLLISION_RADIUS)
		public double collisionRadius;
		@Attribute(Attributes.STRENGTH)
		public double selfPush;
		@Attribute(Attributes.STRENGTH)
		public double otherPush;

		@Attribute(Attributes.SELECTION)
		public double selectRange;
		@Attribute(Attributes.SELECTION)
		public double selectOutOfRange;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airblast");

			cooldown = abilityNode.getNode("cooldown").getLong(1500);
			range = abilityNode.getNode("range").getDouble(25.0);
			speed = abilityNode.getNode("speed").getDouble(1.25);
			collisionRadius = abilityNode.getNode("collision-radius").getDouble(1.0);

			selfPush = abilityNode.getNode("push").getNode("self").getDouble(2.5);
			otherPush = abilityNode.getNode("push").getNode("other").getDouble(3);

			selectRange = abilityNode.getNode("select").getNode("range").getDouble(10.0);
			selectOutOfRange = abilityNode.getNode("select").getNode("out-of-range").getDouble(35.0);

			abilityNode.getNode("speed").setComment("The amount of blocks the blast advances every tick.");
		}
	}
}
