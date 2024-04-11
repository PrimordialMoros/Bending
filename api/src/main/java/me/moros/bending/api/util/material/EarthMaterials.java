/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.util.material;

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockTag;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemTag;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.FeaturePermissions;
import me.moros.bending.api.util.KeyUtil;

/**
 * Group and categorize all earth bendable materials.
 */
public final class EarthMaterials {
  public static final ItemTag METAL_KEYS = ItemTag.reference(KeyUtil.simple("metal_keys"));

  public static final BlockTag EARTH_BENDABLE = BlockTag.reference(KeyUtil.simple("earth_sources"));
  public static final BlockTag SAND_BENDABLE = BlockTag.reference(KeyUtil.simple("sand_sources"));
  public static final BlockTag METAL_BENDABLE = BlockTag.reference(KeyUtil.simple("metal_sources"));
  public static final BlockTag LAVA_BENDABLE = BlockTag.reference(KeyUtil.simple("lava_sources"));
  public static final BlockTag MUD_BENDABLE = BlockTag.reference(KeyUtil.simple("mud_sources"));
  private static final BlockTag ALL = BlockTag.reference(KeyUtil.simple("all_earth_sources"));

  public static void init() {
    BlockTag.builder(EARTH_BENDABLE.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_earth_sources")))
      .add(BlockTag.DIRT)
      .add(BlockTag.BASE_STONE_OVERWORLD)
      .add(BlockTag.BASE_STONE_NETHER)
      .add(BlockTag.STONE_BRICKS)
      .add(BlockTag.TERRACOTTA)
      .add(BlockTag.COAL_ORES).add(BlockTag.IRON_ORES).add(BlockTag.GOLD_ORES).add(BlockTag.COPPER_ORES)
      .add(BlockTag.REDSTONE_ORES).add(BlockTag.LAPIS_ORES).add(BlockTag.DIAMOND_ORES).add(BlockTag.EMERALD_ORES)
      .add(BlockType.NETHER_QUARTZ_ORE)
      .add(BlockTag.CONCRETE_POWDER).contains("concrete")
      .add(BlockType.DIRT_PATH, BlockType.GRAVEL, BlockType.CLAY, BlockType.COBBLESTONE, BlockType.MOSSY_COBBLESTONE,
        BlockType.COBBLESTONE_STAIRS, BlockType.MOSSY_COBBLESTONE_STAIRS, BlockType.STONE_BRICK_STAIRS,
        BlockType.CALCITE, BlockType.SMOOTH_BASALT, BlockType.AMETHYST_BLOCK, BlockType.QUARTZ_BLOCK,
        BlockType.POLISHED_GRANITE, BlockType.POLISHED_DIORITE, BlockType.POLISHED_ANDESITE)
      .buildAndRegister();

    BlockTag.builder(SAND_BENDABLE.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_sand_sources")))
      .add(BlockTag.SAND).endsWith("sandstone")
      .buildAndRegister();

    BlockTag.builder(METAL_BENDABLE.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_metal_sources")))
      .add(BlockType.IRON_BLOCK, BlockType.RAW_IRON_BLOCK,
        BlockType.GOLD_BLOCK, BlockType.RAW_GOLD_BLOCK,
        BlockType.COPPER_BLOCK, BlockType.RAW_COPPER_BLOCK
      ).buildAndRegister();

    BlockTag.builder(LAVA_BENDABLE.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_lava_sources")))
      .add(BlockType.LAVA, BlockType.MAGMA_BLOCK)
      .buildAndRegister();

    BlockTag.builder(MUD_BENDABLE.key())
      .add(BlockTag.reference(KeyUtil.simple("extra_mud_sources")))
      .add(BlockType.SOUL_SAND, BlockType.SOUL_SOIL, BlockType.BROWN_TERRACOTTA).endsWith("mud")
      .buildAndRegister();

    BlockTag.builder(ALL.key())
      .add(EARTH_BENDABLE).add(SAND_BENDABLE).add(METAL_BENDABLE).add(LAVA_BENDABLE).add(MUD_BENDABLE)
      .buildAndRegister();

    ItemTag.builder(METAL_KEYS.key())
      .add(ItemTag.reference(KeyUtil.simple("extra_metal_keys")))
      .add(Item.IRON_INGOT, Item.GOLD_INGOT, Item.COPPER_INGOT, Item.NETHERITE_INGOT)
      .buildAndRegister();
  }

  private EarthMaterials() {
  }

  public static boolean isEarthbendable(User user, Block block) {
    if (isMetalBendable(block) && !user.hasPermission(FeaturePermissions.METAL)) {
      return false;
    }
    if (isLavaBendable(block) && !user.hasPermission(FeaturePermissions.LAVA)) {
      return false;
    }
    return ALL.isTagged(block);
  }

  public static boolean isEarthNotLava(User user, Block block) {
    if (isLavaBendable(block)) {
      return false;
    }
    if (isMetalBendable(block) && !user.hasPermission(FeaturePermissions.METAL)) {
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
