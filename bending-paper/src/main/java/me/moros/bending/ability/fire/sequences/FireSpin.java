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

package me.moros.bending.ability.fire.sequences;

import me.moros.bending.ability.common.ParticleStream;
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
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.VectorMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class FireSpin implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private final Set<Entity> affectedEntities = new HashSet<>();
	private final List<ParticleStream> streams = new ArrayList<>();

	@Override
	public boolean activate(User user, ActivationMethod method) {
		if (method != ActivationMethod.SEQUENCE) return false;
		this.user = user;

		if (!Game.getProtectionSystem().canBuild(user, user.getLocation().toLocation(user.getWorld()).getBlock())) {
			return false;
		}
		recalculateConfig();

		user.setCooldown(this, userConfig.cooldown);

		Vector3 origin = user.getLocation().add(Vector3.PLUS_J);

		Rotation rotation = new Rotation(Vector3.PLUS_J, FastMath.PI / 18, RotationConvention.VECTOR_OPERATOR);
		VectorMethods.rotate(Vector3.PLUS_I, rotation, 36).forEach(
			v -> streams.add(new FireStream(user, new Ray(origin, v.scalarMultiply(userConfig.range))))
		);
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {

	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public List<Collider> getColliders() {
		return streams.stream().map(ParticleStream::getCollider).collect(Collectors.toList());
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "FireSpin";
	}

	private class FireStream extends ParticleStream {
		public FireStream(User user, Ray ray) {
			super(user, ray, userConfig.speed, userConfig.collisionRadius);
			livingOnly = true;
			canCollide = Block::isLiquid;
		}

		@Override
		public void render() {
			ParticleUtil.createFire(user, getBukkitLocation()).count(1).offset(0.15, 0.15, 0.15).spawn();
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(12) == 0) {
				SoundUtil.FIRE_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			if (entity instanceof LivingEntity && !affectedEntities.contains(entity)) {
				DamageUtil.damageEntity(entity, user, userConfig.damage, getDescription());
				FireTick.LARGER.apply(entity, 30);
				affectedEntities.add(entity);
			}
			return true;
		}

		@Override
		public boolean onBlockHit(Block block) {
			return true;
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
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "fire", "sequences", "firekick");

			cooldown = abilityNode.getNode("cooldown").getLong(6000);
			damage = abilityNode.getNode("damage").getDouble(3.0);
			range = abilityNode.getNode("range").getDouble(7.0);
			speed = abilityNode.getNode("speed").getDouble(0.3);
			collisionRadius = abilityNode.getNode("collision-radius").getDouble(0.5);

			abilityNode.getNode("speed").setComment("How many blocks the streams advance with each tick.");
		}
	}
}
