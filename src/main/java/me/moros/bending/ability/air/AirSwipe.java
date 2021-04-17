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
import me.moros.bending.game.AbilityInitializer;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AirSwipe extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Set<Entity> affectedEntities = new HashSet<>();
	private final List<AirStream> streams = new ArrayList<>();

	private boolean charging;
	private double factor = 1;
	private long startTime;

	public AirSwipe(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		startTime = System.currentTimeMillis();
		charging = true;
		if (user.getHeadBlock().isLiquid()) {
			return false;
		}

		for (AirSwipe swipe : Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, AirSwipe.class).collect(Collectors.toList())) {
			if (swipe.charging) {
				swipe.launch();
				return false;
			}
		}
		if (method == ActivationMethod.ATTACK) {
			launch();
		}
		removalPolicy = Policies.builder()
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.add(Policies.IN_LIQUID)
			.build();

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
		if (charging) {
			if (user.isSneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
				ParticleUtil.createAir(user.getMainHandSide().toLocation(user.getWorld())).spawn();
			} else if (!user.isSneaking()) {
				launch();
			}
		} else {
			streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		}

		return (charging || !streams.isEmpty()) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	private void launch() {
		double timeFactor = (System.currentTimeMillis() - startTime) / (double) userConfig.maxChargeTime;
		factor = FastMath.max(1, FastMath.min(userConfig.chargeFactor, timeFactor * userConfig.chargeFactor));
		charging = false;
		user.setCooldown(getDescription(), userConfig.cooldown);
		Vector3 origin = user.getMainHandSide();
		Vector3 dir = user.getDirection();
		Vector3 rotateAxis = dir.crossProduct(Vector3.PLUS_J).normalize().crossProduct(dir);
		Rotation rotation = new Rotation(rotateAxis, FastMath.PI / 36, RotationConvention.VECTOR_OPERATOR);
		int steps = userConfig.arc / 5;
		VectorMethods.createArc(dir, rotation, steps).forEach(
			v -> streams.add(new AirStream(new Ray(origin, v.scalarMultiply(userConfig.range))))
		);
		removalPolicy = Policies.builder().build();
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		Ability collidedAbility = collision.getCollidedAbility();
		if (factor == userConfig.chargeFactor && collision.shouldRemoveSelf()) {
			String name = collidedAbility.getDescription().getName();
			if (AbilityInitializer.layer2.contains(name)) {
				collision.setRemoveCollided(true);
			} else {
				collision.setRemoveSelf(false);
			}
		}
		if (collidedAbility instanceof AirSwipe) {
			double collidedFactor = ((AirSwipe) collidedAbility).factor;
			if (factor > collidedFactor + 0.1) collision.setRemoveSelf(false);
		}
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return streams.stream().map(ParticleStream::getCollider).collect(Collectors.toList());
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private class AirStream extends ParticleStream {
		public AirStream(Ray ray) {
			super(user, ray, userConfig.speed, 0.5);
			canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b) || MaterialUtil.BREAKABLE_PLANTS.isTagged(b);
			livingOnly = false;
		}

		@Override
		public void render() {
			ParticleUtil.createAir(getBukkitLocation()).spawn();

		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.AIR_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(@NonNull Entity entity) {
			if (!affectedEntities.contains(entity)) {
				DamageUtil.damageEntity(entity, user, userConfig.damage * factor, getDescription());
				Vector3 velocity = EntityMethods.getEntityCenter(entity).subtract(ray.origin).normalize().scalarMultiply(factor);
				entity.setVelocity(velocity.clampVelocity());
				affectedEntities.add(entity);
				return true;
			}
			return false;
		}

		@Override
		public boolean onBlockHit(@NonNull Block block) {
			if (BlockMethods.tryBreakPlant(block) || BlockMethods.tryExtinguishFire(user, block)) return false;
			BlockMethods.tryCoolLava(user, block);
			return true;
		}
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.RANGE)
		public int range;
		@Attribute(Attribute.SPEED)
		public double speed;
		public int arc;
		@Attribute(Attribute.CHARGE_TIME)
		public long maxChargeTime;
		@Attribute(Attribute.STRENGTH)
		public double chargeFactor;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airswipe");

			cooldown = abilityNode.node("cooldown").getLong(1500);
			damage = abilityNode.node("damage").getDouble(1.5);
			range = abilityNode.node("range").getInt(14);
			speed = abilityNode.node("speed").getDouble(0.8);
			arc = abilityNode.node("arc").getInt(35);

			chargeFactor = abilityNode.node("charge").node("factor").getDouble(3.0);
			maxChargeTime = abilityNode.node("charge").node("max-time").getLong(2500);

			abilityNode.node("arc").comment("How large the entire arc is in degrees");

			abilityNode.node("charge").node("factor").comment("How much the damage and knockback are multiplied by at full charge");
			abilityNode.node("charge").node("max-time").comment("How many milliseconds it takes to fully charge");
		}
	}
}
