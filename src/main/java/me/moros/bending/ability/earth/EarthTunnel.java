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

package me.moros.bending.ability.earth;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.removal.Policies;
import me.moros.bending.model.predicates.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.methods.VectorMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;
import java.util.function.Predicate;

public class EarthTunnel implements Ability {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;
	private RemovalPolicy removalPolicy;

	private Predicate<Block> predicate;
	private Vector3 center;

	private double distance = 0;
	private int radius = 0;
	private int angle = 0;

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		if (Bending.getGame().getAbilityManager(user.getWorld()).hasAbility(user, EarthTunnel.class)) return false;

		this.user = user;
		recalculateConfig();

		predicate = b -> EarthMaterials.isEarthbendable(user, b) && !EarthMaterials.isLavaBendable(b);
		Optional<Block> block = SourceUtil.getSource(user, userConfig.range, predicate);
		if (!block.isPresent()) return false;

		center = new Vector3(block.get()).add(Vector3.HALF);
		removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();

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
		for (int i = 0; i < 2; i++) {
			if (distance > userConfig.range) {
				return UpdateResult.REMOVE;
			}
			Vector3 offset = VectorMethods.getOrthogonal(user.getDirection(), FastMath.toRadians(angle), radius);
			Block current = center.add(offset).toBlock(user.getWorld());
			if (!Bending.getGame().getProtectionSystem().canBuild(user, current)) {
				return UpdateResult.REMOVE;
			}
			if (predicate.test(current)) {
				TempBlock.create(current, Material.AIR, userConfig.regen, true);
			}
			if (angle >= 360) {
				angle = 0;
				Optional<Block> block = SourceUtil.getSource(user, userConfig.range, predicate);
				if (!block.isPresent()) return UpdateResult.REMOVE;
				center = new Vector3(block.get()).add(Vector3.HALF);

				if (++radius > userConfig.radius) {
					radius = 0;
					if (++distance > userConfig.range) {
						return UpdateResult.REMOVE;
					}
				}
			} else {
				if (radius <= 0) {
					radius++;
				} else {
					angle += 360 / (radius * 16);
				}
			}
		}
		return UpdateResult.CONTINUE;
	}

	@Override
	public void onDestroy() {
		user.setCooldown(getDescription(), userConfig.cooldown);
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull String getName() {
		return "EarthTunnel";
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.COOLDOWN)
		public long cooldown;
		@Attribute(Attributes.RANGE)
		public double range;
		@Attribute(Attributes.RADIUS)
		public int radius;
		@Attribute(Attributes.DURATION)
		public long regen;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "earthtunnel");

			cooldown = abilityNode.getNode("cooldown").getLong(2000);
			range = abilityNode.getNode("range").getDouble(10.0);
			radius = abilityNode.getNode("radius").getInt(1);
			regen = abilityNode.getNode("revert-time").getLong(0);
		}
	}
}

