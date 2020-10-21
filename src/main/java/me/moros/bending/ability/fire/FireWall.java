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

package me.moros.bending.ability.fire;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.FireTick;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class FireWall implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Collection<Block> blocks;
	private OBB collider;

	private boolean applyDamage;
	private long nextRenderTime;

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		double yaw = user.getEntity().getLocation().getYaw();
		double pitch = user.getEntity().getLocation().getPitch();

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

		Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.toRadians(yaw), RotationConvention.VECTOR_OPERATOR);
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
			for (Block block : blocks) {
				Location location = block.getLocation().add(0.5, 0.5, 0.5);
				ParticleUtil.createFire(user, location).count(3).offset(0.6, 0.6, 0.6).spawn();
				ParticleUtil.create(Particle.SMOKE_NORMAL, location).count(1).offset(0.6, 0.6, 0.6).spawn();
				if (ThreadLocalRandom.current().nextInt(12) == 0) {
					SoundUtil.FIRE_SOUND.play(location);
				}
			}
			nextRenderTime = time + 250;
			if (applyDamage) {
				CollisionUtil.handleEntityCollisions(user, collider, entity -> {
					DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
					FireTick.ACCUMULATE.apply(entity, 20);
					return true;
				}, true);
			}
			applyDamage = !applyDamage;
		}

		return UpdateResult.CONTINUE;
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
	public void onDestroy() {
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull String getName() {
		return "FireWall";
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(collider);
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
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
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "firewall");

			cooldown = abilityNode.getNode("cooldown").getLong(11000);
			height = abilityNode.getNode("height").getDouble(4.0);
			width = abilityNode.getNode("width").getDouble(6.0);
			range = abilityNode.getNode("range").getDouble(3.0);
			damage = abilityNode.getNode("damage").getDouble(3.0);
			duration = abilityNode.getNode("duration").getLong(5000);
		}
	}
}
