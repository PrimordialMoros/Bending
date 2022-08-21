/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util.material;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import me.moros.bending.model.user.User;
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;

/**
 * Group and categorize all earth bendable materials.
 */
public final class EarthMaterials {
  public static final MaterialSetTag EARTH_BENDABLE;
  public static final MaterialSetTag SAND_BENDABLE;
  public static final MaterialSetTag METAL_BENDABLE;
  public static final MaterialSetTag LAVA_BENDABLE;
  public static final MaterialSetTag MUD_BENDABLE;
  private static final MaterialSetTag ALL;

  static {
    NamespacedKey key = Metadata.NSK_MATERIAL;
    EARTH_BENDABLE = new MaterialSetTag(key)
      .add(Tag.DIRT.getValues())
      .add(Tag.STONE_BRICKS.getValues())
      .add(MaterialTags.TERRACOTTA.getValues())
      .add(MaterialTags.CONCRETES.getValues())
      .add(MaterialTags.CONCRETE_POWDER.getValues())
      .add(Material.DIRT_PATH,
        Material.GRANITE, Material.POLISHED_GRANITE, Material.DIORITE, Material.POLISHED_DIORITE,
        Material.ANDESITE, Material.POLISHED_ANDESITE, Material.GRAVEL, Material.CLAY,
        Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
        Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
        Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
        Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
        Material.NETHER_QUARTZ_ORE, Material.NETHER_GOLD_ORE, Material.NETHERRACK, Material.STONE_BRICK_STAIRS,
        Material.STONE, Material.COBBLESTONE, Material.COBBLESTONE_STAIRS, Material.AMETHYST_BLOCK,
        Material.DEEPSLATE, Material.CALCITE, Material.TUFF, Material.SMOOTH_BASALT
      ).lock();

    SAND_BENDABLE = new MaterialSetTag(key)
      .add(Material.SAND, Material.RED_SAND)
      .add(MaterialTags.SANDSTONES.getValues())
      .add(MaterialTags.RED_SANDSTONES.getValues()).lock();

    METAL_BENDABLE = new MaterialSetTag(key).add(
      Material.IRON_BLOCK, Material.RAW_IRON_BLOCK,
      Material.GOLD_BLOCK, Material.RAW_GOLD_BLOCK,
      Material.COPPER_BLOCK, Material.RAW_COPPER_BLOCK,
      Material.QUARTZ_BLOCK
    ).lock();

    LAVA_BENDABLE = new MaterialSetTag(key).add(Material.LAVA, Material.MAGMA_BLOCK).lock();

    MUD_BENDABLE = new MaterialSetTag(key).add(Material.SOUL_SAND, Material.SOUL_SOIL, Material.BROWN_TERRACOTTA)
      .endsWith("MUD").lock();

    ALL = new MaterialSetTag(key)
      .add(EARTH_BENDABLE.getValues())
      .add(SAND_BENDABLE.getValues())
      .add(METAL_BENDABLE.getValues())
      .add(LAVA_BENDABLE.getValues())
      .add(MUD_BENDABLE.getValues())
      .lock();
  }

  private EarthMaterials() {
  }

  public static boolean isEarthbendable(User user, Block block) {
    if (isMetalBendable(block) && !user.hasPermission("bending.metal")) {
      return false;
    }
    if (isLavaBendable(block) && !user.hasPermission("bending.lava")) {
      return false;
    }
    return ALL.isTagged(block);
  }

  public static boolean isEarthNotLava(User user, Block block) {
    if (isLavaBendable(block)) {
      return false;
    }
    if (isMetalBendable(block) && !user.hasPermission("bending.metal")) {
      return false;
    }
    return ALL.isTagged(block);
  }

  public static boolean isEarthOrSand(Block block) {
    return EARTH_BENDABLE.isTagged(block) || SAND_BENDABLE.isTagged(block);
  }

  public static boolean isSandBendable(Block block) {
    return SAND_BENDABLE.isTagged(block);
  }

  public static boolean isMetalBendable(Block block) {
    return METAL_BENDABLE.isTagged(block);
  }

  public static boolean isLavaBendable(Block block) {
    return LAVA_BENDABLE.isTagged(block);
  }

  public static boolean isMudBendable(Block block) {
    return MUD_BENDABLE.isTagged(block);
  }
}