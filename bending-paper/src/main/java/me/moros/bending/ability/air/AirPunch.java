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

import me.moros.bending.ability.common.ParticleStream;
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
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.UserMethods;
import me.moros.bending.util.methods.VectorMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class AirPunch implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private AirStream stream;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (user.getHeadBlock().isLiquid() || !Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}

		removalPolicy = Policies.builder().build();

		user.setCooldown(this, userConfig.cooldown);
		Vector3 origin = UserMethods.getMainHandSide(user);
		Vector3 lookingDir = user.getDirection().scalarMultiply(userConfig.range);
		stream = new AirStream(user, new Ray(origin, lookingDir), userConfig.collisionRadius);
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
		return stream.update();
	}

	@Override
	public void destroy() {
	}

	@Override
	public Collection<Collider> getColliders() {
		return Collections.singletonList(stream.getCollider());
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "AirPunch";
	}

	private class AirStream extends ParticleStream {
		public AirStream(User user, Ray ray, double collisionRadius) {
			super(user, ray, userConfig.speed, collisionRadius);
			livingOnly = true;
			canCollide = Block::isLiquid;
		}

		@Override
		public void render() {
			Rotation rotation = new Rotation(user.getDirection(), FastMath.PI / 5, RotationConvention.VECTOR_OPERATOR);
			VectorMethods.rotate(Vector3.ONE.scalarMultiply(0.75), rotation, 10).forEach(v ->
				ParticleUtil.create(Particle.CLOUD, getBukkitLocation().add(v.toVector()))
					.count(0).offset(v.getX(), v.getY(), v.getZ()).extra(-0.04).spawn()
			);
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.AIR_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
			return true;
		}

		@Override
		public boolean onBlockHit(Block block) {
			return BlockMethods.extinguish(user, getBukkitLocation());
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.DAMAGE)
		public double damage;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.COLLISION_RADIUS)
		public double collisionRadius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airpunch");

			cooldown = abilityNode.getNode("cooldown").getLong(1500);
			damage = abilityNode.getNode("damage").getDouble(3.0);
			range = abilityNode.getNode("range").getDouble(18.0);
			speed = abilityNode.getNode("speed").getDouble(0.8);
			collisionRadius = abilityNode.getNode("collision-radius").getDouble(1.0);
		}
	}
}
