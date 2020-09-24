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

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.FireTick;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.CompositeRemovalPolicy;
import me.moros.bending.model.predicates.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FireShield implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;
	private Shield shield;
	private long nextRenderTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (user.getHeadBlock().isLiquid() || !Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}

		removalPolicy = CompositeRemovalPolicy.defaults().add(new SwappedSlotsRemovalPolicy(getDescription())).build();

		if (method == ActivationMethod.PUNCH) {
			shield = new DiskShield();
			user.setCooldown(this, userConfig.cooldown);
		} else {
			shield = new SphereShield();
		}

		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (removalPolicy.shouldRemove(user, getDescription())) {
			return UpdateResult.REMOVE;
		}

		long time = System.currentTimeMillis();
		if (time >= nextRenderTime) {
			shield.render();
			nextRenderTime = time + 200;
		}
		CollisionUtil.handleEntityCollisions(user, shield.getCollider(), entity -> {
			FireTick.LARGER.apply(entity, userConfig.fireTicks);
			return false;
		}, true);

		return shield.update();
	}

	@Override
	public void destroy() {
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "FireShield";
	}

	@Override
	public List<Collider> getColliders() {
		return Collections.singletonList(shield.getCollider());
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	private interface Shield {
		UpdateResult update();

		void render();

		Collider getCollider();
	}

	private class DiskShield implements Shield {
		private Disk disk;
		private Vector3 location;
		private final long endTime;

		private DiskShield() {
			endTime = System.currentTimeMillis() + userConfig.diskDuration;
			update();
		}

		@Override
		public UpdateResult update() {
			location = user.getEyeLocation().add(user.getDirection().scalarMultiply(userConfig.diskRange));
			double r = userConfig.diskRadius;
			AABB aabb = new AABB(new Vector3(-r, -r, -1), new Vector3(r, r, 1));
			Vector3 right = UserMethods.getRightSide(user);
			Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.toRadians(user.getEntity().getLocation().getYaw()), RotationConvention.VECTOR_OPERATOR);
			rotation = rotation.applyTo(new Rotation(right, FastMath.toRadians(user.getEntity().getLocation().getPitch()), RotationConvention.VECTOR_OPERATOR));
			disk = new Disk(new OBB(aabb, rotation).addPosition(location), new Sphere(location, userConfig.diskRadius));
			return System.currentTimeMillis() >= endTime ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
		}

		@Override
		public void render() {
			Rotation rotation = new Rotation(user.getDirection(), FastMath.toRadians(20), RotationConvention.VECTOR_OPERATOR);
			double[] array = Vector3.PLUS_J.crossProduct(user.getDirection()).normalize().toArray();
			for (int i = 0; i < 18; i++) {
				for (double j = 0.2; j <= 1; j += 0.2) {
					Vector3 loc = new Vector3(array).scalarMultiply(j * userConfig.diskRadius);
					ParticleUtil.createFire(user, location.add(loc).toLocation(user.getWorld()))
						.offset(0.25, 0.25, 0.25).spawn();
				}
				rotation.applyTo(array, array);
			}
		}

		@Override
		public Collider getCollider() {
			return disk;
		}
	}

	private class SphereShield implements Shield {
		private Sphere sphere;

		private SphereShield() {
			update();
		}

		@Override
		public Collider getCollider() {
			return sphere;
		}

		@Override
		public UpdateResult update() {
			sphere = new Sphere(user.getEyeLocation(), userConfig.shieldRadius);
			return user.isSneaking() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
		}

		@Override
		public void render() {
			for (Block block : WorldMethods.getNearbyBlocks(user.getHeadBlock().getLocation(), userConfig.shieldRadius)) {
				Location loc = block.getLocation().add(0.5, 0.5, 0.5);
				ParticleUtil.createFire(user, loc).offset(0.2, 0.2, 0.2).spawn();
				if (ThreadLocalRandom.current().nextInt(5) == 0) {
					ParticleUtil.create(Particle.SMOKE_NORMAL, loc).offset(0.2, 0.2, 0.2).spawn();
				}
			}
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.DURATION)
		public int fireTicks;
		@Attribute(Attributes.DURATION)
		public long diskDuration;
		@Attribute(Attributes.RADIUS)
		public double diskRadius;
		@Attribute(Attributes.RANGE)
		public double diskRange;

		@Attribute(Attributes.RADIUS)
		public double shieldRadius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "fireshield");

			cooldown = abilityNode.getNode("cooldown").getLong(500);
			fireTicks = abilityNode.getNode("fire-ticks").getInt(40);

			diskDuration = abilityNode.getNode("disk", "duration").getLong(1000);
			diskRadius = abilityNode.getNode("disk", "radius").getDouble(2.0);
			diskRange = abilityNode.getNode("disk", "range").getDouble(1.5);

			shieldRadius = abilityNode.getNode("shield", "radius").getDouble(3.0);
		}
	}
}
