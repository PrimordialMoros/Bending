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

import com.destroystokyo.paper.MaterialSetTag;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.user.User;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.Optional;
import java.util.function.Predicate;

// TODO remake sourcing from bottles

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
	 * @see #getSource(User, double, Predicate, boolean)
	 */
	public static Optional<Block> getSource(@NonNull User user, double range, @NonNull MaterialSetTag materials) {
		return getSource(user, range, materials::isTagged, false);
	}

	/**
	 * @see #getSource(User, double, Predicate, boolean)
	 */
	public static Optional<Block> getSource(@NonNull User user, double range, @NonNull MaterialSetTag materials, boolean ignorePassable) {
		return getSource(user, range, materials::isTagged, ignorePassable);
	}

	/**
	 * @see #getSource(User, double, Predicate, boolean)
	 */
	public static Optional<Block> getSource(@NonNull User user, double range, @NonNull Predicate<Block> predicate) {
		return getSource(user, range, predicate, false);
	}

	/**
	 * Attempts to find a possible source.
	 * @param user the user checking for a source
	 * @param range the max range to check
	 * @param predicate the predicate to check
	 * @param ignorePassable whether to ignore passable blocks and fluids in the ray trace check
	 * @return an Optional source block
	 */
	public static Optional<Block> getSource(@NonNull User user, double range, @NonNull Predicate<Block> predicate, boolean ignorePassable) {
		Location start = user.getEntity().getEyeLocation();
		Vector dir = user.getDirection().toVector();
		FluidCollisionMode mode = ignorePassable ? FluidCollisionMode.NEVER : FluidCollisionMode.ALWAYS;
		RayTraceResult result = user.getWorld().rayTraceBlocks(start, dir, range, mode, ignorePassable);
		if (result == null || result.getHitBlock() == null) return Optional.empty();
		Block block = result.getHitBlock();
		if (!Bending.getGame().getProtectionSystem().canBuild(user, block) || !predicate.test(block) || !TempBlock.isBendable(block)) {
			return Optional.empty();
		}
		return Optional.of(block);
	}

	public static boolean hasFullBottle(@NonNull User user) {
		return user.getInventory().map(i -> i.containsAtLeast(waterBottle, 1)).orElse(false);
	}

	public static boolean hasEmptyBottle(@NonNull User user) {
		return user.getInventory().map(i -> i.containsAtLeast(emptyBottle, 1)).orElse(false);
	}

	public static boolean fillBottle(@NonNull User user) {
		if (!hasEmptyBottle(user)) return false;
		if (user.getInventory().isPresent()) {
			Inventory inventory = user.getInventory().get();
			return inventory.removeItem(emptyBottle).isEmpty() && inventory.addItem(waterBottle).isEmpty();
		}
		return false;
	}

	public static boolean emptyBottle(@NonNull User user) {
		if (!hasFullBottle(user)) return false;
		if (user.getInventory().isPresent()) {
			Inventory inventory = user.getInventory().get();
			return inventory.removeItem(waterBottle).isEmpty() && inventory.addItem(emptyBottle).isEmpty();
		}
		return false;
	}
}
