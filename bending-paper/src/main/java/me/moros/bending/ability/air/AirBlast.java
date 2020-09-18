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
import me.moros.bending.model.ability.Burstable;
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
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AirBlast implements Ability, Burstable {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;
	private Vector3 origin;
	private Vector3 direction;
	private boolean launched;
	private boolean selectedOrigin;
	private long renderInterval;
	private int particleCount;
	private AirStream stream;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		particleCount = 6;

		if (user.getHeadBlock().isLiquid() || !Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
			return false;
		}

		removalPolicy = CompositeRemovalPolicy.defaults()
			.add(new OutOfRangeRemovalPolicy(userConfig.selectOutOfRange, () -> origin))
			.build();

		for (AirBlast blast : Game.getAbilityInstanceManager(user.getWorld()).getPlayerInstances(user, AirBlast.class)) {
			if (!blast.launched) {
				if (method == ActivationMethod.SNEAK) {
					blast.selectOrigin();
					if (!Game.getProtectionSystem().canBuild(user, blast.origin.toLocation(user.getWorld()).getBlock())) {
						Game.getAbilityInstanceManager(user.getWorld()).destroyInstance(user, blast);
					}
				} else {
					blast.launch();
				}
				return false;
			}
		}

		if (method == ActivationMethod.SNEAK) {
			selectOrigin();
			return Game.getProtectionSystem().canBuild(user, origin.toLocation(user.getWorld()).getBlock());
		} else {
			origin = user.getEyeLocation();
			if (!Game.getProtectionSystem().canBuild(user, user.getHeadBlock())) {
				return false;
			}
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
		if (removalPolicy.shouldRemove(user, getDescription())) {
			return UpdateResult.REMOVE;
		}

		if (!launched) {
			if (!getDescription().equals(user.getSelectedAbility().orElse(null))) {
				return UpdateResult.REMOVE;
			}
			ParticleUtil.createAir(origin.toLocation(user.getWorld())).count(4)
				.offset(ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble(), ThreadLocalRandom.current().nextDouble())
				.spawn();
		}

		return (!launched || stream.update() == UpdateResult.CONTINUE) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	@Override
	public void destroy() {
	}

	private void selectOrigin() {
		Ray ray = new Ray(user.getEyeLocation(), user.getDirection().scalarMultiply(userConfig.selectRange));
		origin = new Vector3(WorldMethods.getTarget(user.getWorld(), ray))
			.subtract(user.getDirection().scalarMultiply(0.5));
		selectedOrigin = true;
	}

	private boolean launch() {
		launched = true;
		Ray ray = new Ray(user.getEyeLocation(), user.getDirection().scalarMultiply(userConfig.selectRange));
		Vector3 target = new Vector3(WorldMethods.getTarget(user.getWorld(), ray));
		direction = target.subtract(origin).normalize(Vector3.PLUS_I);
		user.setCooldown(this, userConfig.cooldown);
		stream = new AirStream(user, new Ray(origin, direction.scalarMultiply(userConfig.range)), userConfig.abilityCollisionRadius);
		return true;
	}

	@Override
	public List<Collider> getColliders() {
		if (stream == null) return Collections.emptyList();
		return Collections.singletonList(stream.getCollider());
	}

	@Override
	public void handleCollision(Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Game.getAbilityInstanceManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "AirBlast";
	}

	// Used to initialize the blast for bursts.
	@Override
	public void initialize(User user, Vector3 location, Vector3 direction) {
		this.user = user;
		recalculateConfig();
		selectedOrigin = false;
		launched = true;
		origin = location;
		this.direction = direction;
		removalPolicy = CompositeRemovalPolicy.defaults().build();
		stream = new AirStream(user, new Ray(location, direction), userConfig.abilityCollisionRadius);
	}

	@Override
	public void setRenderInterval(long interval) {
		this.renderInterval = interval;
	}

	@Override
	public void setRenderParticleCount(int count) {
		this.particleCount = count;
	}

	private class AirStream extends ParticleStream {
		private long nextRenderTime;

		public AirStream(User user, Ray ray, double collisionRadius) {
			super(user, ray, userConfig.speed, collisionRadius);
			livingOnly = true;
			canCollide = Block::isLiquid;
		}

		@Override
		public void render() {
			long time = System.currentTimeMillis();
			if (time > nextRenderTime) {
				ParticleUtil.createAir(getBukkitLocation()).count(particleCount)
					.offset(0.275, 0.275, 0.275)
					.spawn();
				nextRenderTime = time + renderInterval;
			}
		}

		@Override
		public void postRender() {
			if (ThreadLocalRandom.current().nextInt(6) == 0) {
				SoundUtil.AIR_SOUND.play(getBukkitLocation());
			}

			// Handle user separately from the general entity collision.
			if (selectedOrigin) {
				if (AABBUtils.getEntityBounds(user.getEntity()).intersects(collider)) {
					onEntityHit(user.getEntity());
				}
			}
		}

		@Override
		public boolean onEntityHit(Entity entity) {
			double factor = entity.equals(user.getEntity()) ? userConfig.selfPush : userConfig.otherPush;
			factor *= 1.0 - (location.distance(origin) / (2 * userConfig.range));
			// Reduce the push if the player is on the ground.
			if (entity.equals(user.getEntity()) && WorldMethods.isOnGround(entity)) {
				factor *= 0.5;
			}
			Vector3 velocity = new Vector3(entity.getVelocity());
			// The strength of the entity's velocity in the direction of the blast.
			double strength = velocity.dotProduct(direction);
			if (strength > factor) {
				velocity = velocity.scalarMultiply(0.5).add(direction.scalarMultiply(strength / 2));
			} else if (strength > factor * 0.5) {
				velocity = velocity.add(direction.scalarMultiply(factor - strength));
			} else {
				velocity = velocity.add(direction.scalarMultiply(factor * 0.5));
			}
			entity.setVelocity(velocity.toVector());
			entity.setFireTicks(0);
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
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.ABILITY_COLLISION_RADIUS)
		public double abilityCollisionRadius;
		@Attribute(Attributes.STRENGTH)
		public double selfPush;
		@Attribute(Attributes.STRENGTH)
		public double otherPush;

		@Attribute(Attributes.SELECTION)
		public double selectRange;
		@Attribute(Attributes.SELECTION)
		public double selectOutOfRange;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airblast");

			cooldown = abilityNode.getNode("cooldown").getLong(1500);
			range = abilityNode.getNode("range").getDouble(25.0);
			speed = abilityNode.getNode("speed").getDouble(1.25);
			abilityCollisionRadius = abilityNode.getNode("ability-collision-radius").getDouble(1.0);

			selfPush = abilityNode.getNode("push").getNode("self").getDouble(2.5);
			otherPush = abilityNode.getNode("push").getNode("other").getDouble(3);

			selectRange = abilityNode.getNode("select").getNode("range").getDouble(10.0);
			selectOutOfRange = abilityNode.getNode("select").getNode("out-of-range").getDouble(35.0);

			abilityNode.getNode("speed").setComment("The amount of blocks the blast advances every tick.");
		}
	}
}
