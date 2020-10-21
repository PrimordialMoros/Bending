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

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.predicates.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.util.FastMath;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class AirShield implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private long currentPoint;
	private long startTime;

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (!Bending.getGame().getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}
		removalPolicy = Policies.builder()
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.add(Policies.NOT_SNEAKING)
			.add(new ExpireRemovalPolicy(userConfig.duration))
			.build();
		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription()) || !Bending.getGame().getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return UpdateResult.REMOVE;
		}
		currentPoint++;
		Vector3 center = getCenter();
		double height = userConfig.radius * 2;
		double spacing = height / 8;
		for (int i = 1; i < 8; ++i) {
			double y = (i * spacing) - userConfig.radius;
			double factor = 1 - (y * y) / (userConfig.radius * userConfig.radius);
			if (factor <= 0.2) continue; // Don't render the end points that are tightly coupled.
			// Offset each stream so they aren't all lined up.
			double x = userConfig.radius * factor * FastMath.cos(i * currentPoint);
			double z = userConfig.radius * factor * FastMath.sin(i * currentPoint);
			Vector3 loc = center.add(new Vector3(x, y, z));
			ParticleUtil.createAir(loc.toLocation(user.getWorld())).count(5).offset(0.2, 0.2, 0.2).spawn();

			if (ThreadLocalRandom.current().nextInt(12) == 0) {
				SoundUtil.AIR_SOUND.play(loc.toLocation(user.getWorld()));
			}
		}

		CollisionUtil.handleEntityCollisions(user, new Sphere(center, userConfig.radius), entity -> {
			Vector3 toEntity = new Vector3(entity.getLocation()).subtract(center);
			Vector3 normal = toEntity.setY(0).normalize();
			double strength = ((userConfig.radius - toEntity.getNorm()) / userConfig.radius) * userConfig.maxPush;
			strength = FastMath.max(0, FastMath.min(1, strength));
			entity.setVelocity(entity.getVelocity().add(normal.scalarMultiply(strength).toVector()));
			return false;
		}, false);

		return UpdateResult.CONTINUE;
	}

	private Vector3 getCenter() {
		return user.getLocation().add(new Vector3(0, 0.9, 0));
	}

	@Override
	public void onDestroy() {
		double factor = userConfig.duration == 0 ? 1 : System.currentTimeMillis() - startTime / (double) userConfig.duration;
		long cooldown = FastMath.min(1000, (long) (factor * userConfig.cooldown));
		user.setCooldown(getDescription(), cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull String getName() {
		return "AirShield";
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(new Sphere(getCenter(), userConfig.radius));
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.DURATION)
		public long duration;
		@Attribute(Attributes.RADIUS)
		@Attribute(Attributes.COLLISION_RADIUS)
		public double radius;
		@Attribute(Attributes.STRENGTH)
		public double maxPush;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airshield");

			cooldown = abilityNode.getNode("cooldown").getLong(4000);
			duration = abilityNode.getNode("duration").getLong(10000);
			radius = abilityNode.getNode("radius").getDouble(4.0);
			maxPush = abilityNode.getNode("max-push").getDouble(3.0);
		}
	}
}
