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
import me.moros.bending.model.predicates.removal.CompositeRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.UserMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class AirSwipe implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;
	private boolean charging;
	private long startTime;
	private double factor = 1.0;
	private final Set<Entity> affectedEntities = new HashSet<>();
	private final List<AirStream> streams = new ArrayList<>();

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		startTime = System.currentTimeMillis();
		charging = true;

		if (user.getHeadBlock().isLiquid() || !Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}

		removalPolicy = CompositeRemovalPolicy.defaults().build();

		for (AirSwipe swipe : Game.getAbilityManager(user.getWorld()).getUserInstances(user, AirSwipe.class).collect(Collectors.toList())) {
			if (swipe.charging) {
				swipe.launch();
				return false;
			}
		}
		if (method == ActivationMethod.PUNCH) {
			launch();
		}

		return true;
	}

	@Override
	public void recalculateConfig() {
		this.userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		if (removalPolicy.shouldRemove(user, getDescription())) {
			return UpdateResult.REMOVE;
		}
		if (charging) {
			if (!getDescription().equals(user.getSelectedAbility().orElse(null))) {
				return UpdateResult.REMOVE;
			}
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
	public void destroy() {
	}

	private void launch() {
		double timeFactor = (System.currentTimeMillis() - startTime) / (double) userConfig.maxChargeTime;
		factor = FastMath.max(1, FastMath.min(userConfig.chargeFactor, timeFactor * userConfig.chargeFactor));
		charging = false;
		user.setCooldown(this, userConfig.cooldown);
		Vector3 origin = UserMethods.getMainHandSide(user);
		Vector3 lookingDir = user.getDirection();
		Vector3 rotateAxis = lookingDir.crossProduct(Vector3.PLUS_J).normalize(Vector3.PLUS_I).crossProduct(lookingDir);
		Rotation rotation = new Rotation(rotateAxis, FastMath.toRadians(userConfig.arcStep), RotationConvention.VECTOR_OPERATOR);
		int steps = userConfig.arc / userConfig.arcStep;
		double[] streamDir = lookingDir.toArray();
		double[] streamDirInverse = lookingDir.toArray();
		// Micro-optimization for constructing the arc with a series of equally spaced out rays
		// We start from the center (direction user is facing) and then fanning outwards
		// This way we can reuse the same rotation object without needing to recalculate the coefficients every time
		streams.add(new AirStream(user, new Ray(origin, lookingDir.scalarMultiply(userConfig.range))));
		for (int i = 0; i < steps / 2; i++) {
			rotation.applyTo(streamDir, streamDir);
			rotation.applyInverseTo(streamDirInverse, streamDirInverse);
			streams.add(new AirStream(user, new Ray(origin, new Vector3(streamDir).scalarMultiply(userConfig.range))));
			streams.add(new AirStream(user, new Ray(origin, new Vector3(streamDirInverse).scalarMultiply(userConfig.range))));
		}
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public List<Collider> getColliders() {
		return streams.stream().map(AirStream::getCollider).collect(Collectors.toList());
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
			super(user, ray, userConfig.speed, userConfig.abilityCollisionRadius);
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
		@Attribute(Attributes.ABILITY_COLLISION_RADIUS)
		public double abilityCollisionRadius;
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
			arc = abilityNode.getNode("arc").getInt(32);
			arcStep = abilityNode.getNode("arc-step").getInt(4);
			abilityCollisionRadius = abilityNode.getNode("ability-collision-radius").getDouble(0.5);

			chargeFactor = abilityNode.getNode("charge").getNode("factor").getDouble(3.0);
			maxChargeTime = abilityNode.getNode("charge").getNode("max-time").getLong(2500);

			abilityNode.getNode("arc").setComment("How large the entire arc is in degrees");
			abilityNode.getNode("arc-step").setComment("How many degrees apart each stream is in the arc");

			abilityNode.getNode("charge").getNode("factor").setComment("How much the damage and knockback are multiplied by at full charge");
			abilityNode.getNode("charge").getNode("max-time").setComment("How many milliseconds it takes to fully charge");
		}
	}
}
