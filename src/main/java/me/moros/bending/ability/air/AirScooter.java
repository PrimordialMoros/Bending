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
import me.moros.atlas.configurate.CommentedConfigurationNode;
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

public class AirScooter extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private HeightSmoother heightSmoother;

	public boolean canRender = true;
	private double verticalPosition = 0;
	private int stuckCount = 0;

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
		if ((dist < 0.5 || dist > 3)) {
			return false;
		}

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

		stuckCount = user.getEntity().getVelocity().lengthSquared() < 0.1 ? stuckCount + 1 : 0;
		if (stuckCount > 10 || !move()) {
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
		for (double theta = 0; theta < 2 * FastMath.PI * 2; theta += FastMath.PI / 5) {
			double sin = FastMath.sin(verticalPosition);
			double x = 0.6 * FastMath.cos(theta) * sin;
			double y = 0.6 * FastMath.cos(verticalPosition);
			double z = 0.6 * FastMath.sin(theta) * sin;
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
		return Collections.singletonList(new Sphere(user.getLocation(), 2));
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private boolean move() {
		if (isColliding()) return false;
		Vector3 direction = user.getDirection().setY(0).normalize();
		double height = WorldMethods.distanceAboveGround(user.getEntity());
		double smoothedHeight = heightSmoother.add(height);
		if (user.getLocBlock().isLiquid()) {
			height = 0.5;
		} else if (smoothedHeight > 3.25) {
			return false;
		}
		double force = FastMath.max(-0.5, FastMath.min(0.5, -0.3 * (height - getPrediction())));
		Vector3 velocity = direction.scalarMultiply(userConfig.speed).setY(force);
		user.getEntity().setVelocity(velocity.clampVelocity());
		user.getEntity().setFallDistance(0);
		return true;
	}

	private boolean isColliding() {
		double speed = user.getEntity().getVelocity().setY(0).length();
		Vector3 direction = user.getDirection().setY(0).normalize(Vector3.ZERO);
		Vector3 front = user.getEyeLocation().subtract(new Vector3(0, 0.5, 0))
			.add(direction.scalarMultiply(FastMath.max(userConfig.speed, speed)));
		Block block = front.toBlock(user.getWorld());
		return !MaterialUtil.isTransparentOrWater(block) || !block.isPassable();
	}

	private double getPrediction() {
		Vector3 currentDirection = user.getDirection().setY(0).normalize();
		double playerSpeed = user.getEntity().getVelocity().setY(0).length();
		double speed = FastMath.max(userConfig.speed, playerSpeed) * 3;
		Vector3 location = user.getLocation().add(currentDirection.scalarMultiply(speed));
		AABB userBounds = AABBUtils.getEntityBounds(user.getEntity());
		for (Block block : BlockMethods.combineFaces(location.toBlock(user.getWorld()))) {
			if (AABBUtils.getBlockBounds(block).intersects(userBounds)) {
				return 2.25;
			}
		}
		return 1.25;
	}

	private static class HeightSmoother {
		private final double[] values;
		private int index;

		private HeightSmoother() {
			index = 0;
			values = new double[10];
		}

		private double add(double value) {
			values[index] = value;
			index = (index + 1) % values.length;
			return get();
		}

		private double get() {
			return Arrays.stream(values).sum() / values.length;
		}
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.SPEED)
		public double speed;
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;

		public Config() {
			super();
		}

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airscooter");

			speed = abilityNode.node("speed").getDouble(0.6);
			cooldown = abilityNode.node("cooldown").getLong(4000);
		}
	}
}
