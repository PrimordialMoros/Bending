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

package me.moros.bending.ability.fire;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.atlas.expiringmap.ExpirationPolicy;
import me.moros.atlas.expiringmap.ExpiringMap;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class FireWall extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Map<Entity, Boolean> affectedEntities = ExpiringMap.builder()
		.expirationPolicy(ExpirationPolicy.CREATED)
		.expiration(200, TimeUnit.MILLISECONDS).build();

	private Collection<Block> blocks;
	private OBB collider;

	private long nextRenderTime;

	public FireWall(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		int pitch = user.getPitch();

		if (FastMath.abs(pitch) > 50) {
			return false;
		}

		double hw = userConfig.width / 2.0;
		double hh = userConfig.height / 2.0;

		AABB aabb = new AABB(new Vector3(-hw, -hh, -0.5), new Vector3(hw, hh, 0.5));
		Vector3 right = user.getDirection().crossProduct(Vector3.PLUS_J).normalize();
		Vector3 location = user.getEyeLocation().add(user.getDirection().scalarMultiply(userConfig.range));
		if (!Bending.getGame().getProtectionSystem().canBuild(user, location.toBlock(user.getWorld()))) {
			return false;
		}

		Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.toRadians(user.getYaw()), RotationConvention.VECTOR_OPERATOR);
		rotation = rotation.applyTo(new Rotation(right, FastMath.toRadians(pitch), RotationConvention.VECTOR_OPERATOR));
		collider = new OBB(aabb, rotation).addPosition(location);
		double radius = collider.getHalfExtents().maxComponent() + 1;
		blocks = WorldMethods.getNearbyBlocks(location.toLocation(user.getWorld()), radius, b -> collider.contains(new Vector3(b)) && MaterialUtil.isTransparent(b));
		if (blocks.isEmpty()) return false;

		removalPolicy = Policies.builder().add(new ExpireRemovalPolicy(userConfig.duration)).build();

		nextRenderTime = 0;
		user.setCooldown(getDescription(), userConfig.cooldown);
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
		long time = System.currentTimeMillis();
		if (time > nextRenderTime) {
			nextRenderTime = time + 250;
			for (Block block : blocks) {
				Location location = block.getLocation().add(0.5, 0.5, 0.5);
				ParticleUtil.createFire(user, location).count(3).offset(0.6, 0.6, 0.6).extra(0.01).spawn();
				ParticleUtil.create(Particle.SMOKE_NORMAL, location).offset(0.6, 0.6, 0.6).spawn();
				if (ThreadLocalRandom.current().nextInt(12) == 0) {
					SoundUtil.FIRE_SOUND.play(location);
				}
			}
		}
		CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, false);
		return UpdateResult.CONTINUE;
	}


	private boolean onEntityHit(Entity entity) {
		if (entity instanceof Arrow) {
			entity.remove();
			return true;
		}
		if (affectedEntities.containsKey(entity)) return false;
		affectedEntities.put(entity, false);
		entity.setVelocity(Vector3.ZERO.toVector());
		DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
		FireTick.ACCUMULATE.apply(entity, 20);
		return true;
	}

	public void setWall(Collection<Block> blocks, OBB collider) {
		if (blocks == null || blocks.isEmpty()) return;
		this.blocks = blocks;
		this.collider = collider;
	}

	public void updateDuration(long duration) {
		removalPolicy = Policies.builder().add(new ExpireRemovalPolicy(duration)).build();
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(collider);
	}

	public double getWidth() {
		return userConfig.width;
	}

	public double getHeight() {
		return userConfig.height;
	}

	public double getRange() {
		return userConfig.range;
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.HEIGHT)
		public double height;
		@Attribute(Attribute.RADIUS)
		public double width;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.RANGE)
		public double range;
		@Attribute(Attribute.DURATION)
		public long duration;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "firewall");

			cooldown = abilityNode.node("cooldown").getLong(11000);
			height = abilityNode.node("height").getDouble(4.0);
			width = abilityNode.node("width").getDouble(6.0);
			range = abilityNode.node("range").getDouble(3.0);
			damage = abilityNode.node("damage").getDouble(3.0);
			duration = abilityNode.node("duration").getLong(5000);
		}
	}
}
