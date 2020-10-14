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

import me.moros.bending.ability.common.basic.ParticleStream;
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
import me.moros.bending.model.predicates.removal.SwappedSlotsRemovalPolicy;
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
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AirSwipe implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private final Set<Entity> affectedEntities = new HashSet<>();
	private final List<AirStream> streams = new ArrayList<>();

	private boolean charging;
	private double factor = 1;
	private long startTime;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		charging = true;
		if (user.getHeadBlock().isLiquid() || !Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}

		for (AirSwipe swipe : Game.getAbilityManager(user.getWorld()).getUserInstances(user, AirSwipe.class).collect(Collectors.toList())) {
			if (swipe.charging) {
				swipe.launch();
				return false;
			}
		}
		if (method == ActivationMethod.PUNCH) {
			launch();
		}
		removalPolicy = Policies.builder()
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.add(Policies.IN_LIQUID)
			.build();

		startTime = System.currentTimeMillis();
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
			if (user.isSneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
				ParticleUtil.createAir(UserMethods.getMainHandSide(user).toLocation(user.getWorld())).spawn();
			} else if (!user.isSneaking()) {
				launch();
			}
		} else {
			streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
		}

		return (charging || !streams.isEmpty()) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	@Override
	public void onDestroy() {
	}

	private void launch() {
		double timeFactor = (System.currentTimeMillis() - startTime) / (double) userConfig.maxChargeTime;
		factor = FastMath.max(1, FastMath.min(userConfig.chargeFactor, timeFactor * userConfig.chargeFactor));
		charging = false;
		user.setCooldown(getDescription(), userConfig.cooldown);
		Vector3 origin = UserMethods.getMainHandSide(user);
		Vector3 dir = user.getDirection();
		Vector3 rotateAxis = dir.crossProduct(Vector3.PLUS_J).normalize().crossProduct(dir);
		Rotation rotation = new Rotation(rotateAxis, FastMath.toRadians(userConfig.arcStep), RotationConvention.VECTOR_OPERATOR);
		int steps = userConfig.arc / userConfig.arcStep;
		VectorMethods.createArc(dir, rotation, steps).forEach(
			v -> streams.add(new AirStream(user, new Ray(origin, v.scalarMultiply(userConfig.range))))
		);
		removalPolicy = Policies.builder().build();
	}

	@Override
	public void onCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public Collection<Collider> getColliders() {
		return streams.stream().map(ParticleStream::getCollider).collect(Collectors.toList());
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "AirSwipe";
	}

	private class AirStream extends ParticleStream {
		public AirStream(User user, Ray ray) {
			super(user, ray, userConfig.speed, userConfig.collisionRadius);
			canCollide = Block::isLiquid;
		}

		@Override
		public void render() {
			ParticleUtil.createAir(getBukkitLocation()).spawn();

		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.AIR_SOUND.play(getBukkitLocation());
			}
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			if (!affectedEntities.contains(entity)) {
				DamageUtil.damageEntity(entity, user, userConfig.damage * factor, getDescription());
				entity.setVelocity(entity.getLocation().subtract(getBukkitLocation()).toVector().normalize().multiply(factor));
				affectedEntities.add(entity);
				return true;
			}
			return false;
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
		public int range;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.COLLISION_RADIUS)
		public double collisionRadius;
		public int arc;
		public int arcStep;
		@Attribute(Attributes.CHARGE_TIME)
		public long maxChargeTime;
		@Attribute(Attributes.STRENGTH)
		public double chargeFactor;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airswipe");

			cooldown = abilityNode.getNode("cooldown").getLong(1500);
			damage = abilityNode.getNode("damage").getDouble(2.0);
			range = abilityNode.getNode("range").getInt(14);
			speed = abilityNode.getNode("speed").getDouble(0.8);
			arc = abilityNode.getNode("arc").getInt(35);
			arcStep = abilityNode.getNode("arc-step").getInt(5);
			collisionRadius = abilityNode.getNode("collision-radius").getDouble(0.5);

			chargeFactor = abilityNode.getNode("charge").getNode("factor").getDouble(3.0);
			maxChargeTime = abilityNode.getNode("charge").getNode("max-time").getLong(2500);

			abilityNode.getNode("arc").setComment("How large the entire arc is in degrees");
			abilityNode.getNode("arc-step").setComment("How many degrees apart each stream is in the arc");

			abilityNode.getNode("charge").getNode("factor").setComment("How much the damage and knockback are multiplied by at full charge");
			abilityNode.getNode("charge").getNode("max-time").setComment("How many milliseconds it takes to fully charge");
		}
	}
}
