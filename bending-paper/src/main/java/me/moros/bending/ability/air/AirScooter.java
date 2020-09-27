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

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.CompositeRemovalPolicy;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//TODO cleanup
public class AirScooter implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private CompositeRemovalPolicy removalPolicy;

	private HeightSmoother heightSmoother;

	private double verticalPosition;
	private int stuckCount;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		if (Game.getAbilityManager(user.getWorld()).destroyInstanceType(user, AirScooter.class)) {
			return false;
		}

		this.user = user;
		recalculateConfig();
		heightSmoother = new HeightSmoother();

		double dist = WorldMethods.distanceAboveGround(user.getEntity());
		// Only activate AirScooter if the player is in the air and near the ground.
		if ((dist < 0.5 || dist > 5) && !user.getLocation().toBlock(user.getWorld()).isLiquid()) {
			return false;
		}

		removalPolicy = CompositeRemovalPolicy.defaults().add(Policies.SNEAKING).build();
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

		if (!Game.getProtectionSystem().canBuild(user, user.getLocBlock(), getDescription())) {
			return UpdateResult.REMOVE;
		}

		if (isColliding()) {
			return UpdateResult.REMOVE;
		}

		if (user.getEntity().getVelocity().lengthSquared() < 0.1) {
			stuckCount++;
		} else {
			stuckCount = 0;
		}

		if (stuckCount > 10) {
			return UpdateResult.REMOVE;
		}

		if (!move()) {
			return UpdateResult.REMOVE;
		}

		render();
		return UpdateResult.CONTINUE;
	}

	@Override
	public void destroy() {
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	public void render() {
		verticalPosition += 0.25 * FastMath.PI;
		for (int i = 0; i < 10; i++) {
			double angle = i * FastMath.PI / 5;
			double x = 0.6 * FastMath.cos(angle) * FastMath.sin(verticalPosition);
			double y = 0.6 * FastMath.cos(verticalPosition);
			double z = 0.6 * FastMath.sin(angle) * FastMath.sin(verticalPosition);
			ParticleUtil.createAir(user.getLocation().toLocation(user.getWorld()).add(x, y, z)).spawn();
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
		return Collections.singletonList(new Sphere(user.getLocation(), userConfig.collisionRadius));
	}

	@Override
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "AirScooter";
	}

	private boolean move() {
		Vector3 direction = user.getDirection().setY(0).normalize();
		// How far the player is above the ground.
		double height = WorldMethods.distanceAboveGround(user.getEntity());
		double maxHeight = 3.25;
		double smoothedHeight = heightSmoother.add(height);
		if (user.getLocation().toBlock(user.getWorld()).isLiquid()) {
			height = 0.5;
		} else {
			// Destroy ability if player gets too far from ground.
			if (smoothedHeight > maxHeight) {
				return false;
			}
		}
		// Calculate the spring force to push the player back to the target height.
		double force = -0.3 * (height - getPrediction());
		if (FastMath.abs(force) > 0.5) {
			force = force > 0 ? 0.5 : -0.5; // Cap the force to maxForce so the player isn't instantly pulled to the ground.
		}
		Vector3 velocity = direction.scalarMultiply(userConfig.speed).setY(force);
		user.getEntity().setVelocity(velocity.clampVelocity().toVector());
		user.getEntity().setFallDistance(0);
		return true;
	}

	private boolean isColliding() {
		double playerSpeed = user.getEntity().getVelocity().setY(0).length();
		Vector3 direction = user.getDirection().setY(0).normalize(Vector3.ZERO);
		// The location in front of the player, where the player will be in one second.
		Vector3 front = user.getEyeLocation().subtract(new Vector3(0.0, 0.5, 0.0))
			.add(direction.scalarMultiply(FastMath.max(userConfig.speed, playerSpeed)));

		Block block = front.toBlock(user.getWorld());
		if (block.getType() == Material.WATER) {
			return false;
		}
		return !MaterialUtil.isTransparent(block) || MaterialUtil.isSolid(block);
	}

	private double getPrediction() {
		Vector3 currentDirection = user.getDirection().setY(0).normalize();
		double playerSpeed = user.getEntity().getVelocity().setY(0).length();
		double s = FastMath.max(userConfig.speed, playerSpeed) * 3;
		Vector3 location = user.getLocation().add(currentDirection.scalarMultiply(s));
		AABB playerBounds = AABBUtils.getEntityBounds(user.getEntity());
		// Project the player forward and check all surrounding blocks for collision.
		for (Block block : BlockMethods.combineFaces(location.toBlock(user.getWorld()))) {
			if (AABBUtils.getBlockBounds(block).intersects(playerBounds)) {
				// Player will collide with a block soon, so try to raise the player over it.
				return 2.25;
			}
		}
		return 1.25;
	}

	private static class HeightSmoother {
		private final double[] values;
		private int index;

		public HeightSmoother() {
			index = 0;
			values = new double[10];
		}

		public double add(double value) {
			values[index] = value;
			index = (index + 1) % values.length;
			return get();
		}

		public double get() {
			return Arrays.stream(values).sum() / values.length;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.SPEED)
		public double speed;
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.COLLISION_RADIUS)
		public double collisionRadius;

		public Config() {
			super();
		}

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "air", "airscooter");

			speed = abilityNode.getNode("speed").getDouble(0.6);
			cooldown = abilityNode.getNode("cooldown").getLong(4000);
			collisionRadius = abilityNode.getNode("collision-radius").getDouble(2.0);
		}
	}
}
