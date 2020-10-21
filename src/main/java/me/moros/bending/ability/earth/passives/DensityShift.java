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

package me.moros.bending.ability.earth.passives;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.commented.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.function.Predicate;

public class DensityShift implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	@Override
	public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
	}

	@Override
	public @NonNull UpdateResult update() {
		return UpdateResult.CONTINUE;
	}

	public static boolean isSoftened(User user) {
		if (!Bending.getGame().getAbilityRegistry().getAbilityDescription("DensityShift").map(user::canBend).orElse(false)) {
			return false;
		}
		DensityShift instance = Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, DensityShift.class).orElse(null);
		if (instance == null) {
			return false;
		}
		long duration = instance.userConfig.duration;
		Block block = user.getLocBlock().getRelative(BlockFace.DOWN);
		if (EarthMaterials.isEarthbendable(user, block)) {
			Predicate<Block> predicate = b -> EarthMaterials.isEarthbendable(user, b) && b.getRelative(BlockFace.UP).isPassable();
			WorldMethods.getNearbyBlocks(block.getLocation().add(0.5, 0.5, 0.5), instance.userConfig.radius, predicate)
				.forEach(b -> TempBlock.create(b, MaterialUtil.getSoftType(b.getBlockData()), duration, true));
			return true;
		}
		return false;
	}

	@Override
	public void onDestroy() {
	}

	@Override
	public @NonNull User getUser() {
		return user;
	}

	@Override
	public @NonNull String getName() {
		return "DensityShift";
	}

	@Override
	public void onCollision(@NonNull Collision collision) {
	}

	public static class Config extends Configurable {
		@Attribute(Attribute.DURATION)
		public long duration;
		@Attribute(Attribute.RADIUS)
		public double radius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "passives", "densityshift");

			duration = abilityNode.getNode("duration").getLong(6000);
			radius = abilityNode.getNode("radius").getDouble(2.0);
		}
	}
}

