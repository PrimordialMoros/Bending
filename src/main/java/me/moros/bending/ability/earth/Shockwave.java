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

package me.moros.bending.ability.earth;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.atlas.expiringmap.ExpirationPolicy;
import me.moros.atlas.expiringmap.ExpiringMap;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Shockwave extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Collection<Ripple> streams = new ArrayList<>();
	private final Set<Entity> affectedEntities = new HashSet<>();
	private final Set<Block> affectedBlocks = new HashSet<>();
	private final Map<Block, Boolean> recentAffectedBlocks = ExpiringMap.builder()
		.expirationPolicy(ExpirationPolicy.CREATED)
		.expiration(1000, TimeUnit.MILLISECONDS).build();
	private Vector3 origin;

	private boolean released;
	private double range;
	private long startTime;

	public Shockwave(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (!Bending.getGame().getProtectionSystem().canBuild(user, user.getLocBlock())) {
			return false;
		}
		removalPolicy = Policies.builder().add(new SwappedSlotsRemovalPolicy(getDescription())).build();
		released = false;
		if (method == ActivationMethod.FALL) {
			if (user.getEntity().getFallDistance() < userConfig.fallThreshold || user.isSneaking()) {
				return false;
			}
			release(false);
		}

		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		if (!released) {
			boolean charged = isCharged();
			if (charged) {
				ParticleUtil.create(Particle.SMOKE_NORMAL, UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
				if (!user.isSneaking()) {
					release(false);
				}
			} else {
				if (!user.isSneaking()) return UpdateResult.REMOVE;
			}
			return UpdateResult.CONTINUE;
		}

		if (!recentAffectedBlocks.isEmpty()) {
			CollisionUtil.handleEntityCollisions(user, new Sphere(origin, range + 2), this::onEntityHit, false);
		}

		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	public boolean onEntityHit(@NonNull Entity entity) {
		if (!affectedEntities.contains(entity)) {
			boolean inRange = false;
			if (recentAffectedBlocks.containsKey(entity.getLocation().getBlock())) {
				inRange = true;
			} else if (entity instanceof LivingEntity) {
				Block eyeBlock = ((LivingEntity) entity).getEyeLocation().getBlock();
				if (recentAffectedBlocks.containsKey(eyeBlock)) {
					inRange = true;
				}
			}

			Vector3 loc = new Vector3(entity.getLocation());

			if (!inRange) {
				for (Block block : recentAffectedBlocks.keySet()) {
					AABB blockBounds = AABB.BLOCK_BOUNDS.grow(new Vector3(0.5, 1, 0.5)).at(new Vector3(block));
					if (blockBounds.intersects(AABBUtils.getEntityBounds(entity))) {
						inRange = true;
						break;
					}
				}
			}

			if (inRange) {
				DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
				double deltaY = FastMath.min(0.9, 0.6 + loc.distance(origin) / (1.5 * range));
				Vector3 push = loc.subtract(origin).normalize().setY(deltaY).scalarMultiply(userConfig.knockback);
				entity.setVelocity(push.clampVelocity().toVector());
				affectedEntities.add(entity);
			}
		}
		return false;
	}

	public boolean isCharged() {
		return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
	}

	public static void activateCone(User user) {
		if (user.getSelectedAbility().map(AbilityDescription::getName).orElse("").equals("Shockwave")) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, Shockwave.class)
				.ifPresent(s -> s.release(true));
		}
	}

	private void release(boolean cone) {
		if (released || !isCharged()) return;
		released = true;
		range = cone ? userConfig.coneRange : userConfig.ringRange;

		double deltaAngle = FastMath.PI / (3 * range);
		origin = user.getLocation().add(Vector3.MINUS_J);
		Vector3 dir = user.getDirection().setY(0).normalize();
		Rotation rotation = new Rotation(Vector3.PLUS_J, deltaAngle, RotationConvention.VECTOR_OPERATOR);
		double speed = cone ? userConfig.coneSpeed : userConfig.ringSpeed;
		if (cone) {
			VectorMethods.createArc(dir, rotation, NumberConversions.round(range / 2)).forEach(v ->
				streams.add(new Ripple(new Ray(origin.add(v).floor(), v), speed))
			);
		} else {
			VectorMethods.rotate(dir, rotation, NumberConversions.round(range * 6)).forEach(v ->
				streams.add(new Ripple(new Ray(origin.add(v).floor(), v), speed))
			);
		}
		removalPolicy = Policies.builder().build();
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public User getUser() {
		return user;
	}

	private class Ripple implements Updatable {
		private final Ray ray;

		private Vector3 location;

		private final double speed;

		public Ripple(Ray ray, double speed) {
			this.location = ray.origin.add(new Vector3(0.5, 1.25, 0.5));
			this.ray = ray;
			this.speed = FastMath.min(1, speed);
		}

		@Override
		public @NonNull UpdateResult update() {
			location = location.add(ray.direction.scalarMultiply(speed));
			Block baseBlock = location.toBlock(user.getWorld()).getRelative(BlockFace.DOWN);

			if (!isValidBlock(baseBlock)) {
				if (isValidBlock(baseBlock.getRelative(BlockFace.UP))) {
					location = location.add(Vector3.PLUS_J);
				} else if (isValidBlock(baseBlock.getRelative(BlockFace.DOWN))) {
					location = location.add(Vector3.MINUS_J);
				} else {
					return UpdateResult.REMOVE;
				}
			}

			if (location.distanceSq(ray.origin) > range * range) {
				return UpdateResult.REMOVE;
			}

			if (affectedBlocks.contains(baseBlock)) {
				return UpdateResult.CONTINUE;
			}

			if (!Bending.getGame().getProtectionSystem().canBuild(user, location.toBlock(user.getWorld()))) {
				return UpdateResult.REMOVE;
			}

			Block block = location.toBlock(user.getWorld());
			if (!affectedBlocks.contains(block)) {
				affectedBlocks.add(block);
				recentAffectedBlocks.put(block, false);
				double deltaY = FastMath.min(0.35, 0.1 + location.distance(ray.origin) / (1.5 * range));
				Vector3 velocity = new Vector3(0, deltaY, 0);
				new BendingFallingBlock(block, block.getRelative(BlockFace.DOWN).getBlockData(), velocity, true, 3000);
				if (ThreadLocalRandom.current().nextInt(6) == 0) {
					SoundUtil.EARTH_SOUND.play(block.getLocation());
				}
			}

			return UpdateResult.CONTINUE;
		}

		protected boolean isValidBlock(Block block) {
			if (!MaterialUtil.isTransparent(block.getRelative(BlockFace.UP))) return false;
			return EarthMaterials.isEarthbendable(user, block) && !block.isLiquid();
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.CHARGE_TIME)
		public long chargeTime;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.STRENGTH)
		public double knockback;

		@Attribute(Attribute.RANGE)
		public double coneRange;
		@Attribute(Attribute.SPEED)
		public double coneSpeed;

		@Attribute(Attribute.RANGE)
		public double ringRange;
		@Attribute(Attribute.SPEED)
		public double ringSpeed;

		public double fallThreshold;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "shockwave");

			cooldown = abilityNode.getNode("cooldown").getLong(8000);
			chargeTime = abilityNode.getNode("charge-time").getInt(2500);
			damage = abilityNode.getNode("damage").getDouble(4.0);
			knockback = abilityNode.getNode("knockback").getDouble(1.2);

			coneRange = abilityNode.getNode("cone", "range").getDouble(14.0);
			coneSpeed = abilityNode.getNode("cone", "speed").getDouble(1.0);

			ringRange = abilityNode.getNode("ring", "range").getDouble(9.0);
			ringSpeed = abilityNode.getNode("ring", "speed").getDouble(0.8);

			fallThreshold = abilityNode.getNode("fall-threshold").getDouble(12.0);
		}
	}
}
