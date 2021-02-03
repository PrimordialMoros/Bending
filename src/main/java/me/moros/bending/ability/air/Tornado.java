/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.concurrent.ThreadLocalRandom;

public class Tornado extends AbilityInstance implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private double yOffset = 0;
	private long startTime;

	public Tornado(@NonNull AbilityDescription desc) {
		super(desc);
	}

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		removalPolicy = Policies.builder()
			.add(new SwappedSlotsRemovalPolicy(getDescription()))
			.add(Policies.NOT_SNEAKING)
			.add(new ExpireRemovalPolicy(userConfig.duration))
			.build();
		startTime = System.currentTimeMillis();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		if (removalPolicy.test(user, getDescription()) || user.getHeadBlock().isLiquid()) {
			return UpdateResult.REMOVE;
		}

		if (!Bending.getGame().getProtectionSystem().canBuild(user, user.getLocBlock())) {
			return UpdateResult.REMOVE;
		}
		Vector3 base = WorldMethods.getTarget(user.getWorld(), user.getRay(userConfig.range), false);
		Block baseBlock = base.toBlock(user.getWorld());
		if (MaterialUtil.isTransparent(baseBlock.getRelative(BlockFace.DOWN))) {
			return UpdateResult.CONTINUE;
		}
		if (!Bending.getGame().getProtectionSystem().canBuild(user, baseBlock)) {
			return UpdateResult.REMOVE;
		}

		long time = System.currentTimeMillis();
		double factor = FastMath.min(1, time - startTime / userConfig.growthTime);
		double height = 2 + factor * (userConfig.height - 2);
		double radius = 2 + factor * (userConfig.radius - 2);

		AABB box = new AABB(new Vector3(-radius, 0, -radius), new Vector3(radius, height, radius)).at(base);
		CollisionUtil.handleEntityCollisions(user, box, entity -> {
			double dy = entity.getLocation().getY() - base.getY();
			double r = 2 + (radius - 2) * dy;
			Vector3 delta = VectorMethods.getEntityCenter(entity).subtract(base);
			double distSq = delta.getX() * delta.getX() + delta.getZ() * delta.getZ();
			if (distSq > r * r) return false;

			if (entity.equals(user.getEntity())) {
				double velY;
				if (dy >= height * .95) {
					velY = 0;
				} else if (dy >= height * .85) {
					velY = 6.0 * (.95 - dy / height);
				} else {
					velY = 0.6;
				}
				Vector3 velocity = user.getDirection().setY(velY).scalarMultiply(factor);
				entity.setVelocity(velocity.clampVelocity());
			} else {
				Vector3 normal = delta.setY(0).normalize();
				Vector3 ortho = normal.crossProduct(Vector3.PLUS_J).normalize();
				Vector3 velocity = ortho.add(normal).normalize().scalarMultiply(factor);
				entity.setVelocity(velocity.clampVelocity());
			}
			return false;
		}, true, true);

		render(base, factor, height, radius);
		return UpdateResult.CONTINUE;
	}

	private void render(Vector3 base, double factor, double height, double radius) {
		double amount = FastMath.min(30, FastMath.max(4, factor * 30));
		yOffset += 0.1;
		if (yOffset >= 1) yOffset = 0;
		for (int i = 0; i < 3; i++) {
			double offset = i * 2 * FastMath.PI / 3.0;
			for (double y = yOffset; y < height; y += (height / amount)) {
				double r = 2 + (radius - 2) * y / height;
				double x = r * FastMath.cos(y + offset);
				double z = r * FastMath.sin(y + offset);
				Location loc = base.add(new Vector3(x, y, z)).toLocation(user.getWorld());
				ParticleUtil.createAir(loc).spawn();
				if (ThreadLocalRandom.current().nextInt(20) == 0) {
					SoundUtil.AIR_SOUND.play(loc);
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	private static class Config extends Configurable {
		@Attribute(Attribute.COOLDOWN)
		public long cooldown;
		@Attribute(Attribute.DURATION)
		public long duration;
		@Attribute(Attribute.RADIUS)
		public double radius;
		@Attribute(Attribute.HEIGHT)
		public double height;
		@Attribute(Attribute.RANGE)
		public double range;
		public long growthTime;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.node("abilities", "air", "tornado");

			cooldown = abilityNode.node("cooldown").getLong(4000);
			duration = abilityNode.node("duration").getLong(8000);
			radius = abilityNode.node("radius").getDouble(10.0);
			height = abilityNode.node("height").getDouble(15.0);
			range = abilityNode.node("range").getDouble(25.0);
			growthTime = abilityNode.node("growth-time").getLong(2000);
		}
	}
}
