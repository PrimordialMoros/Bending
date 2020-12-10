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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

//TODO cleanup
public class AirScooter extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private HeightSmoother heightSmoother;

	public boolean canRender;
	private double verticalPosition;
	private int stuckCount;

	public AirScooter(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();

		if (Policies.IN_LIQUID.test(user, getDescription())) return false;

		heightSmoother = new HeightSmoother();

		double dist = WorldMethods.distanceAboveGround(user.getEntity());
		// Only activate AirScooter if the player is in the air and near the ground.
		if ((dist < 0.5 || dist > 5) && !user.getLocBlock().isLiquid()) {
			return false;
		}

		canRender = true;
		removalPolicy = Policies.builder().add(Policies.SNEAKING).build();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription())) {
			return UpdateResult.REMOVE;
		}

		if (!Bending.getGame().getProtectionSystem().canBuild(user, user.getLocBlock(), getDescription())) {
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

		if (canRender) render();
		if (ThreadLocalRandom.current().nextInt(4) == 0) {
			SoundUtil.AIR_SOUND.play(user.getEntity().getLocation());
		}

		return UpdateResult.CONTINUE;
	}

	@Override
	public void onDestroy() {
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	private void render() {
		verticalPosition += 0.25 * FastMath.PI;
		for (int i = 0; i < 10; i++) {
			double angle = i * FastMath.PI / 5;
			double x = 0.6 * FastMath.cos(angle) * FastMath.sin(verticalPosition);
			double y = 0.6 * FastMath.cos(verticalPosition);
			double z = 0.6 * FastMath.sin(angle) * FastMath.sin(verticalPosition);
			ParticleUtil.createAir(user.getEntity().getLocation().add(x, y, z)).spawn();
		}
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
		if (collision.shouldRemoveFirst()) {
			Bending.getGame().getAbilityManager(user.getWorld()).destroyInstance(user, this);
		}
	}

	@Override
	public @NonNull Collection<@NonNull Collider> getColliders() {
		return Collections.singletonList(new Sphere(user.getLocation(), userConfig.collisionRadius));
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private boolean move() {
		Vector3 direction = user.getDirection().setY(0).normalize();
		// How far the player is above the ground.
		double height = WorldMethods.distanceAboveGround(user.getEntity());
		double maxHeight = 3.25;
		double smoothedHeight = heightSmoother.add(height);
		if (user.getLocBlock().isLiquid()) {
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
		double speed = user.getEntity().getVelocity().setY(0).length();
		Vector3 direction = user.getDirection().setY(0).normalize(Vector3.ZERO);
		// The location in front of the player, where the player will be in one second.
		Vector3 front = user.getEyeLocation().subtract(new Vector3(0.0, 0.5, 0.0))
			.add(direction.scalarMultiply(FastMath.max(userConfig.speed, speed)));

		Block block = front.toBlock(user.getWorld());
		if (MaterialUtil.isWater(block)) {
			return false;
		}
		return !MaterialUtil.isTransparent(block) || !block.isPassable();
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
		@Attribute(Attribute.SPEED)
		public double speed;
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.COLLISION_RADIUS)
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
