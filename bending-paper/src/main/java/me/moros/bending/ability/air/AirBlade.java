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

import me.moros.bending.ability.air.sequences.*;
import me.moros.bending.ability.common.AbstractWheel;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.CompositeRemovalPolicy;
import me.moros.bending.model.predicates.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.VectorMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class AirBlade implements Ability {
	public static final Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;

	private Vector3 origin;
	private Vector3 direction;
	private Blade blade;

	private boolean charging;
	private double factor = 1;
	private long startTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		if (Game.getAbilityManager(user.getWorld()).hasAbility(user, AirBlade.class)) {
			return false;
		}

		this.user = user;
		recalculateConfig();

		charging = true;
		direction = user.getDirection().setY(0).normalize();
		double maxRadius = userConfig.radius * userConfig.chargeFactor * 0.5;
		origin = user.getLocation().add(direction).add(new Vector3(0, maxRadius, 0));

		removalPolicy = CompositeRemovalPolicy.defaults()
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.add(new OutOfRangeRemovalPolicy(userConfig.prepareRange, () -> origin))
			.add(Policies.IN_LIQUID)
			.build();

		startTime = System.currentTimeMillis();

		AirWheel wheel = Game.getAbilityManager(user.getWorld()).getFirstInstance(user, AirWheel.class).orElse(null);
		if (wheel != null) {
			origin = wheel.getCenter();
			factor = userConfig.chargeFactor;
			charging = false;
			blade = new Blade(user, new Ray(origin, direction), userConfig.speed * factor * 0.5);
			removalPolicy = CompositeRemovalPolicy.defaults()
				.add(new OutOfRangeRemovalPolicy(userConfig.range * factor, origin, () -> blade.getLocation())).build();
			user.setCooldown(this, userConfig.cooldown);
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, wheel);
			return true;
		}

		if (method == ActivationMethod.SNEAK_RELEASE) {
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

		if (charging) {
			if (origin.toBlock(user.getWorld()).isLiquid()) {
				return UpdateResult.REMOVE;
			}
			long time = System.currentTimeMillis();
			if (user.isSneaking() && time > startTime + 100) {
				double timeFactor = FastMath.min(0.9, (time - startTime) / (double) userConfig.maxChargeTime);
				Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(direction);
				Rotation rotation = new Rotation(rotateAxis, FastMath.PI / 10, RotationConvention.VECTOR_OPERATOR);
				double r = userConfig.radius * userConfig.chargeFactor * timeFactor * 0.5;
				VectorMethods.rotate(direction.scalarMultiply(r), rotation, 20).forEach(v ->
					ParticleUtil.createAir(origin.add(v).toLocation(user.getWorld())).spawn()
				);
			} else if (!user.isSneaking()) {
				launch();
			}
			return UpdateResult.CONTINUE;
		}

		return blade.update();
	}

	private void launch() {
		double timeFactor = FastMath.min(1, (System.currentTimeMillis() - startTime) / (double) userConfig.maxChargeTime);
		factor = FastMath.max(1, timeFactor * userConfig.chargeFactor);
		charging = false;
		blade = new Blade(user, new Ray(origin, direction));
		removalPolicy = CompositeRemovalPolicy.defaults()
			.add(new OutOfRangeRemovalPolicy(userConfig.range * factor, origin, () -> blade.getLocation())).build();
		user.setCooldown(this, userConfig.cooldown);
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
		return "AirBlade";
	}

	@Override
	public Collection<Collider> getColliders() {
		return Collections.singletonList(blade.getCollider());
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	private class Blade extends AbstractWheel {
		public Blade(User user, Ray ray) {
			super(user, ray, userConfig.radius * factor * 0.5, userConfig.speed * factor * 0.5);
		}

		// When started from wheel
		public Blade(User user, Ray ray, double speed) {
			super(user, ray, 1.6, speed);
		}

		@Override
		public void render() {
			Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(this.ray.direction);
			Rotation rotation = new Rotation(rotateAxis, FastMath.PI / 20, RotationConvention.VECTOR_OPERATOR);
			VectorMethods.rotate(this.ray.direction.scalarMultiply(this.radius), rotation, 40).forEach(v ->
				ParticleUtil.createAir(location.add(v).toLocation(user.getWorld())).spawn()
			);
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.AIR_SOUND.play(location.toLocation(user.getWorld()));
			}
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			DamageUtil.damageEntity(entity, user, userConfig.damage * factor);
			return true;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.RADIUS)
		public double radius;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.RANGE)
		public double prepareRange;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.CHARGE_TIME)
		public long maxChargeTime;
		@Attribute(Attributes.STRENGTH)
		public double chargeFactor;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airblade");

			cooldown = abilityNode.getNode("cooldown").getLong(4000);
			radius = abilityNode.getNode("radius").getDouble(1.2);
			damage = abilityNode.getNode("damage").getDouble(2.0);
			range = abilityNode.getNode("range").getDouble(12.0);
			prepareRange = abilityNode.getNode("prepare-range").getDouble(8.0);
			speed = abilityNode.getNode("speed").getDouble(0.8);

			chargeFactor = abilityNode.getNode("charge").getNode("factor").getDouble(3.0);
			maxChargeTime = abilityNode.getNode("charge").getNode("max-time").getLong(2000);

			abilityNode.getNode("speed").setComment("How many blocks the blade advances every tick.");
			abilityNode.getNode("charge").getNode("factor").setComment("How much the damage and range are multiplied by at full charge. Radius and speed are only affected by half that amount.");
			abilityNode.getNode("charge").getNode("max-time").setComment("How many milliseconds it takes to fully charge");

		}
	}
}

