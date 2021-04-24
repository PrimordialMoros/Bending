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

package me.moros.bending.ability.earth;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.atlas.expiringmap.ExpirationPolicy;
import me.moros.atlas.expiringmap.ExpiringMap;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractBlockLine;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
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
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
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
		.expiration(500, TimeUnit.MILLISECONDS).build();
	private Vector3 origin;

	private boolean released;
	private double range;
	private long startTime;

	public Shockwave(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(User user, ActivationMethod method) {
		if (method == ActivationMethod.ATTACK) {
			Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, Shockwave.class)
				.ifPresent(s -> s.release(true));
			return false;
		}

		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, Shockwave.class)) return false;

		this.user = user;
		recalculateConfig();

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
				ParticleUtil.create(Particle.SMOKE_NORMAL, user.getMainHandSide().toLocation(user.getWorld())).spawn();
				if (!user.isSneaking()) {
					release(false);
				}
			} else {
				if (!user.isSneaking()) return UpdateResult.REMOVE;
			}
			return UpdateResult.CONTINUE;
		}

		if (!recentAffectedBlocks.isEmpty()) {
			Set<Block> positions = new HashSet<>(recentAffectedBlocks.keySet());
			CollisionUtil.handleEntityCollisions(user, new Sphere(origin, range + 2), e -> onEntityHit(e, positions), false);
		}

		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	private boolean onEntityHit(Entity entity, Set<Block> positions) {
		if (!affectedEntities.contains(entity)) {
			boolean inRange = false;
			if (positions.contains(entity.getLocation().getBlock())) {
				inRange = true;
			} else if (entity instanceof LivingEntity) {
				Block eyeBlock = ((LivingEntity) entity).getEyeLocation().getBlock();
				if (positions.contains(eyeBlock)) {
					inRange = true;
				}
			}

			Vector3 loc = new Vector3(entity.getLocation());

			if (!inRange) {
				for (Block block : positions) {
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
				entity.setVelocity(push.clampVelocity());
				affectedEntities.add(entity);
			}
		}
		return false;
	}

	private boolean isCharged() {
		return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
	}

	private void release(boolean cone) {
		if (released || !isCharged() || !user.isOnGround()) return;
		released = true;
		range = cone ? userConfig.coneRange : userConfig.ringRange;

		double deltaAngle = FastMath.PI / (3 * range);
		origin = user.getLocation().floor().add(Vector3.HALF);
		Vector3 dir = user.getDirection().setY(0).normalize();
		Rotation rotation = new Rotation(Vector3.PLUS_J, deltaAngle, RotationConvention.VECTOR_OPERATOR);
		if (cone) {
			VectorMethods.createArc(dir, rotation, NumberConversions.ceil(range / 1.5)).forEach(v ->
				streams.add(new Ripple(new Ray(origin, v.scalarMultiply(range)), 0))
			);
		} else {
			VectorMethods.rotate(dir, rotation, NumberConversions.ceil(range * 6)).forEach(v ->
				streams.add(new Ripple(new Ray(origin, v.scalarMultiply(range)), 75))
			);
		}

		// First update in same tick to only apply cooldown if there are valid ripples
		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		if (streams.isEmpty()) {
			removalPolicy = (u, d) -> true;
		} else {
			removalPolicy = Policies.builder().build();
			user.setCooldown(getDescription(), userConfig.cooldown);
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	private class Ripple extends AbstractBlockLine {
		public Ripple(Ray ray, long interval) {
			super(user, ray);
			this.interval = interval;
		}

		@Override
		public boolean isValidBlock(@NonNull Block block) {
			if (block.isLiquid() || !MaterialUtil.isTransparent(block)) return false;
			return EarthMaterials.isEarthbendable(user, block.getRelative(BlockFace.DOWN));
		}

		@Override
		public void render(@NonNull Block block) {
			if (affectedBlocks.contains(block)) return;
			affectedBlocks.add(block);
			recentAffectedBlocks.put(block, false);
			double deltaY = FastMath.min(0.25, 0.05 + location.distance(ray.origin) / (3 * range));
			Vector3 velocity = new Vector3(0, deltaY, 0);
			BlockData data = block.getRelative(BlockFace.DOWN).getBlockData();
			new BendingFallingBlock(block, data, velocity, true, 450);
			ParticleUtil.create(Particle.BLOCK_CRACK, block.getLocation().add(0.5, 1.25, 0.5))
				.count(5).offset(0.5, 0.25, 0.5).data(data).spawn();
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.EARTH_SOUND.play(block.getLocation());
			}
		}
	}

	private static class Config extends Configurable {
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
		@Attribute(Attribute.RANGE)
		public double ringRange;
		public double fallThreshold;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "shockwave");

			cooldown = abilityNode.node("cooldown").getLong(8000);
			chargeTime = abilityNode.node("charge-time").getInt(2500);
			damage = abilityNode.node("damage").getDouble(3.0);
			knockback = abilityNode.node("knockback").getDouble(1.2);
			coneRange = abilityNode.node("cone-range").getDouble(14.0);
			ringRange = abilityNode.node("ring-range").getDouble(9.0);
			fallThreshold = abilityNode.node("fall-threshold").getDouble(12.0);
		}
	}
}
