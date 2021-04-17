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
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collection;
import java.util.Collections;

public class FireShield extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Shield shield;

	private long nextRenderTime;

	public FireShield(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (user.getHeadBlock().isLiquid()) {
			return false;
		}

		if (method == ActivationMethod.SNEAK) {
			shield = new SphereShield();
			removalPolicy = Policies.builder()
				.add(new SwappedSlotsRemovalPolicy(getDescription()))
				.add(new ExpireRemovalPolicy(userConfig.shieldDuration))
				.add(Policies.NOT_SNEAKING).build();
		} else {
			shield = new DiskShield();
			removalPolicy = Policies.builder()
				.add(new SwappedSlotsRemovalPolicy(getDescription()))
				.add(new ExpireRemovalPolicy(userConfig.diskDuration)).build();
		}

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
		if (time >= nextRenderTime) {
			shield.render();
			nextRenderTime = time + 200;
		}
		CollisionUtil.handleEntityCollisions(user, shield.getCollider(), entity -> {
			FireTick.LARGER.apply(entity, userConfig.fireTicks);
			return false;
		});

		shield.update();
		return UpdateResult.CONTINUE;
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(shield.getCollider());
	}

	private interface Shield {
		void update();

		void render();

		Collider getCollider();
	}

	private class DiskShield implements Shield {
		private Disk disk;
		private Vector3 location;

		private DiskShield() {
			update();
		}

		@Override
		public void update() {
			location = user.getEyeLocation().add(user.getDirection().scalarMultiply(userConfig.diskRange));
			double r = userConfig.diskRadius;
			AABB aabb = new AABB(new Vector3(-r, -r, -1), new Vector3(r, r, 1));
			Vector3 right = user.getRightSide();
			Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.toRadians(user.getYaw()), RotationConvention.VECTOR_OPERATOR);
			rotation = rotation.applyTo(new Rotation(right, FastMath.toRadians(user.getPitch()), RotationConvention.VECTOR_OPERATOR));
			disk = new Disk(new OBB(aabb, rotation).addPosition(location), new Sphere(location, userConfig.diskRadius));
		}

		@Override
		public void render() {
			Rotation rotation = new Rotation(user.getDirection(), FastMath.toRadians(20), RotationConvention.VECTOR_OPERATOR);
			double[] array = Vector3.PLUS_J.crossProduct(user.getDirection()).normalize().toArray();
			for (int i = 0; i < 18; i++) {
				for (double j = 0.2; j <= 1; j += 0.2) {
					Vector3 loc = new Vector3(array).scalarMultiply(j * userConfig.diskRadius);
					ParticleUtil.createFire(user, location.add(loc).toLocation(user.getWorld()))
						.offset(0.2, 0.2, 0.2).extra(0.01).spawn();
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
		public void update() {
			sphere = new Sphere(user.getEyeLocation(), userConfig.shieldRadius);
		}

		@Override
		public void render() {
			for (Block block : WorldMethods.getNearbyBlocks(user.getHeadBlock().getLocation(), userConfig.shieldRadius)) {
				Location loc = block.getLocation().add(0.5, 0.5, 0.5);
				ParticleUtil.createFire(user, loc).offset(0.2, 0.2, 0.2).extra(0.01).spawn();
			}
		}
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DURATION)
		public int fireTicks;
		@Attribute(Attribute.DURATION)
		public long diskDuration;
		@Attribute(Attribute.RADIUS)
		public double diskRadius;
		@Attribute(Attribute.RANGE)
		public double diskRange;

		@Attribute(Attribute.DURATION)
		public long shieldDuration;
		@Attribute(Attribute.RADIUS)
		public double shieldRadius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "fireshield");

			cooldown = abilityNode.node("cooldown").getLong(2000);
			fireTicks = abilityNode.node("fire-ticks").getInt(40);

			diskDuration = abilityNode.node("disk", "duration").getLong(1000);
			diskRadius = abilityNode.node("disk", "radius").getDouble(2.0);
			diskRange = abilityNode.node("disk", "range").getDouble(1.5);

			shieldDuration = abilityNode.node("shield", "duration").getLong(10000);
			shieldRadius = abilityNode.node("shield", "radius").getDouble(3.0);
		}
	}
}
