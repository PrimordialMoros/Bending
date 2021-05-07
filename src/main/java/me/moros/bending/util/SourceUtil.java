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

package me.moros.bending.util;

import java.util.Optional;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.user.User;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

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
   * Attempts to find a possible source.
   * @param user the user checking for a source
   * @param range the max range to check
   * @param predicate the predicate to check
   * @return an Optional source block
   */
  public static Optional<Block> find(@NonNull User user, double range, @NonNull Predicate<Block> predicate) {
    BlockIterator it = new BlockIterator(user.entity(), Math.min(100, NumberConversions.ceil(range)));
    while (it.hasNext()) {
      Block block = it.next();
      if (block.getType().isAir()) {
        continue;
      }
      if (predicate.test(block) && TempBlock.isBendable(block) && Bending.game().protectionSystem().canBuild(user, block)) {
        return Optional.of(block);
      }
      if (!block.isPassable()) {
        break;
      }
    }
    return Optional.empty();
  }
}
