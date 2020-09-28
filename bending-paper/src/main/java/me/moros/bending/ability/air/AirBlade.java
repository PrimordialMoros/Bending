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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AirBlade implements Ability {
	public static Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;

	private Blade blade;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		Vector3 direction = user.getDirection().setY(0).normalize();
		Vector3 location = user.getLocation().add(direction);
		location = location.add(new Vector3(0, userConfig.radius, 0));
		if (location.toBlock(user.getWorld()).isLiquid()) return false;

		blade = new Blade(user, new Ray(location, direction));
		if (!blade.resolveMovement(userConfig.radius)) return false;

		removalPolicy = CompositeRemovalPolicy.defaults()
			.add(new OutOfRangeRemovalPolicy(userConfig.range, location, () -> blade.getLocation())).build();

		user.setCooldown(this, userConfig.cooldown);
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
		return blade.update();
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
	public List<Collider> getColliders() {
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
			super(user, ray, userConfig.radius, userConfig.speed);
		}

		@Override
		public void render() {
			Vector3 rotateAxis = Vector3.PLUS_J.crossProduct(this.ray.direction).normalize();
			Rotation rotation = new Rotation(rotateAxis, FastMath.PI / 36, RotationConvention.VECTOR_OPERATOR);
			VectorMethods.rotate(this.ray.direction.scalarMultiply(this.radius), rotation, 72).forEach(v ->
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
			DamageUtil.damageEntity(entity, user, userConfig.damage);
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
		@Attribute(Attributes.SPEED)
		public double speed;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airblade");

			cooldown = abilityNode.getNode("cooldown").getLong(4000);
			radius = abilityNode.getNode("radius").getDouble(1.8);
			damage = abilityNode.getNode("damage").getDouble(3.0);
			range = abilityNode.getNode("range").getDouble(16.0);
			speed = abilityNode.getNode("speed").getDouble(1.1);

			abilityNode.getNode("speed").setComment("How many blocks the blade advances every tick.");
		}
	}
}

