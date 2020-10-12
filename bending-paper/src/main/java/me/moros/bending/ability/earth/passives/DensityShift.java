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

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.function.Predicate;

public class DensityShift implements PassiveAbility {
	private static final Config config = new Config();

	private User user;
	private Config userConfig;

	@Override
	public boolean activate(User user, ActivationMethod method) {
		this.user = user;
		recalculateConfig();
		return true;
	}

	@Override
	public void recalculateConfig() {
		userConfig = Game.getAttributeSystem().calculate(this, config);
	}

	@Override
	public UpdateResult update() {
		return UpdateResult.CONTINUE;
	}

	public static boolean isSoftened(User user) {
		if (!Game.getAbilityRegistry().getAbilityDescription("DensityShift").map(user::canBend).orElse(false)) {
			return false;
		}
		DensityShift instance = Game.getAbilityManager(user.getWorld()).getFirstInstance(user, DensityShift.class).orElse(null);
		if (instance == null) {
			return false;
		}
		long duration = instance.userConfig.duration;
		Block block = user.getLocBlock().getRelative(BlockFace.DOWN);
		if (MaterialUtil.isEarthbendable(user, block)) {
			Predicate<Block> predicate = b -> MaterialUtil.isEarthbendable(user, b) && b.getRelative(BlockFace.UP).isPassable();
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
	public User getUser() {
		return user;
	}

	@Override
	public String getName() {
		return "DensityShift";
	}

	@Override
	public void onCollision(Collision collision) {
	}

	public static class Config extends Configurable {
		@Attribute(Attributes.DURATION)
		public long duration;
		@Attribute(Attributes.RADIUS)
		public double radius;

		@Override
		public void onConfigReload() {
			CommentedConfigurationNode abilityNode = config.getNode("abilities", "earth", "passives", "densityshift");

			duration = abilityNode.getNode("duration").getLong(6000);
			radius = abilityNode.getNode("radius").getDouble(2.0);
		}
	}
}

