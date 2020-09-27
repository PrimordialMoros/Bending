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

package me.moros.bending.util;

import me.moros.bending.ability.water.util.*;
import me.moros.bending.game.Game;
import me.moros.bending.model.user.User;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.Set;

/**
 * Utility class to handle bending sourcing.
 */
public final class SourceUtil {
	private static final ItemStack emptyBottle = new ItemStack(Material.POTION);
	private static final ItemStack waterBottle;

	static {
		waterBottle = new ItemStack(Material.POTION, 1);
		PotionMeta potionMeta = (PotionMeta) waterBottle.getItemMeta();
		potionMeta.setBasePotionData(new PotionData(PotionType.WATER, false, false));
		waterBottle.setItemMeta(potionMeta);
	}

	/**
	 * Attempts to find a possible source.
	 * @param user the user checking for a source
	 * @param range the max range to check
	 * @param materials a set of valid source materials
	 * @return an Optional source block
	 */
	public static Optional<Block> getSource(User user, double range, Set<Material> materials) {
		Location start = user.getEyeLocation().toLocation(user.getWorld());
		Vector dir = user.getDirection().toVector();
		RayTraceResult result = user.getWorld().rayTraceBlocks(start, dir, range, FluidCollisionMode.ALWAYS, false);
		if (result == null || result.getHitBlock() == null) return Optional.empty();
		Block block = result.getHitBlock();
		if (!Game.getProtectionSystem().canBuild(user, block) || !materials.contains(block.getType()) || !isBendableTempBlock(block)) {
			return Optional.empty();
		}
		return Optional.of(block);
	}

	private static boolean isBendableTempBlock(Block block) {
		return true; //TODO implement this as functionality inside tempblock
	}

	public static boolean hasFullBottle(User user) {
		return user.getInventory().map(i -> i.containsAtLeast(waterBottle, 1)).orElse(false);
	}

	public static boolean hasEmptyBottle(User user) {
		return user.getInventory().map(i -> i.containsAtLeast(emptyBottle, 1)).orElse(false);
	}

	public static boolean fillBottle(User user) {
		if (!BottleReturn.config.enabled || !hasEmptyBottle(user)) return false;
		if (user.getInventory().isPresent()) {
			return user.getInventory().get().removeItem(emptyBottle).isEmpty() && user.getInventory().get().addItem(waterBottle).isEmpty();
		}
		return false;
	}

	public static boolean emptyBottle(User user) {
		if (!BottleReturn.config.enabled || !hasFullBottle(user)) return false;
		if (Game.getAbilityManager(user.getWorld()).hasAbility(user, BottleReturn.class)) return false;
		if (user.getInventory().isPresent()) {
			return user.getInventory().get().removeItem(waterBottle).isEmpty() && user.getInventory().get().addItem(emptyBottle).isEmpty();
		}
		return false;
	}
}
