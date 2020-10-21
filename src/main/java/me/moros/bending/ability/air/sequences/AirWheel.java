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

package me.moros.bending.ability.air.sequences;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.air.*;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;

import java.util.Collection;
import java.util.Collections;

public class AirWheel implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	private AirScooter scooter;
	private Collider collider;
	private Vector3 center;

	private long nextRenderTime;
	private long nextDamageTime;

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		scooter = new AirScooter();
		if (user.isOnCooldown(scooter.getDescription()) || !scooter.activate(user, ActivationMethod.PUNCH))
			return false;
		scooter.canRender = false;

		this.user = user;
		recalculateConfig();

		user.setCooldown(scooter.getDescription(), 1000); // Ensures airscooter won't be activated twice
		collider = new Sphere(user.getLocation(), userConfig.collisionRadius);
		nextRenderTime = 0;
		nextDamageTime = 0;
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		long time = System.currentTimeMillis();
		center = user.getLocation().add(new Vector3(0, 0.8, 0)).add(user.getDirection().setY(0).scalarMultiply(1.2));
		collider = new Sphere(center, userConfig.collisionRadius);

		if (time > nextRenderTime) {
			render();
			nextRenderTime = time + 100;
		}

		if (time > nextDamageTime) {
			CollisionUtil.handleEntityCollisions(user, collider, entity -> {
				DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
				return false;
			}, true);
			nextDamageTime = time + 500;
		}
		return scooter.update();
	}

	@Override
	public void onDestroy() {
		scooter.onDestroy();
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	private void render() {
		Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(user.getDirection().setY(0));
		Rotation rotation = new Rotation(rotateAxis, FastMath.PI / 20, RotationConvention.VECTOR_OPERATOR);
		VectorMethods.rotate(user.getDirection().scalarMultiply(1.6), rotation, 40).forEach(v ->
			ParticleUtil.createAir(center.add(v).toLocation(user.getWorld())).spawn()
		);
	}

	public Vector3 getCenter() {
		return center;
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(collider);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull String getName() {
		return "AirWheel";
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DAMAGE)
		public double damage;
		@Attribute(Attribute.COLLISION_RADIUS)
		public double collisionRadius;

		public Config() {
			super();
		}

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "sequences", "airwheel");

			cooldown = abilityNode.getNode("cooldown").getLong(4000);
			damage = abilityNode.getNode("damage").getDouble(2.0);
			collisionRadius = abilityNode.getNode("collision-radius").getDouble(2.0);
		}
	}
}

