/*
 * Copyright 2020-2023 Moros
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
import me.moros.bending.api.util.KeyUtil;

/**
 * Group and categorize all earth bendable materials.
 */
public final class EarthMaterials {
  public static final ItemTag METAL_KEYS = ItemTag.reference(KeyUtil.simple("metal-keys"));

  public static final BlockTag EARTH_BENDABLE = BlockTag.reference(KeyUtil.simple("earth-sources"));
  public static final BlockTag SAND_BENDABLE = BlockTag.reference(KeyUtil.simple("sand-sources"));
  public static final BlockTag METAL_BENDABLE = BlockTag.reference(KeyUtil.simple("metal-sources"));
  public static final BlockTag LAVA_BENDABLE = BlockTag.reference(KeyUtil.simple("lava-sources"));
  public static final BlockTag MUD_BENDABLE = BlockTag.reference(KeyUtil.simple("mud-sources"));
  private static final BlockTag ALL = BlockTag.reference(KeyUtil.simple("all-earth-sources"));

  public static void init() {
    BlockTag.builder(EARTH_BENDABLE.key())
      .add(BlockTag.DIRT)
      .add(BlockTag.STONE_BRICKS)
      .add(BlockTag.TERRACOTTA)
      .contains("concrete")
      .add(BlockType.DIRT_PATH,
        BlockType.GRANITE, BlockType.POLISHED_GRANITE, BlockType.DIORITE, BlockType.POLISHED_DIORITE,
        BlockType.ANDESITE, BlockType.POLISHED_ANDESITE, BlockType.GRAVEL, BlockType.CLAY,
        BlockType.COAL_ORE, BlockType.DEEPSLATE_COAL_ORE, BlockType.IRON_ORE, BlockType.DEEPSLATE_IRON_ORE,
        BlockType.GOLD_ORE, BlockType.DEEPSLATE_GOLD_ORE, BlockType.REDSTONE_ORE, BlockType.DEEPSLATE_REDSTONE_ORE,
        BlockType.LAPIS_ORE, BlockType.DEEPSLATE_LAPIS_ORE, BlockType.DIAMOND_ORE, BlockType.DEEPSLATE_DIAMOND_ORE,
        BlockType.COPPER_ORE, BlockType.DEEPSLATE_COPPER_ORE, BlockType.EMERALD_ORE, BlockType.DEEPSLATE_EMERALD_ORE,
        BlockType.NETHER_QUARTZ_ORE, BlockType.NETHER_GOLD_ORE, BlockType.NETHERRACK, BlockType.STONE_BRICK_STAIRS,
        BlockType.STONE, BlockType.COBBLESTONE, BlockType.COBBLESTONE_STAIRS, BlockType.AMETHYST_BLOCK,
        BlockType.DEEPSLATE, BlockType.CALCITE, BlockType.TUFF, BlockType.SMOOTH_BASALT
      ).buildAndRegister();
    BlockTag.builder(SAND_BENDABLE.key()).add(BlockTag.SAND).endsWith("sandstone").buildAndRegister();
    BlockTag.builder(METAL_BENDABLE.key())
      .add(BlockType.IRON_BLOCK, BlockType.RAW_IRON_BLOCK,
        BlockType.GOLD_BLOCK, BlockType.RAW_GOLD_BLOCK,
        BlockType.COPPER_BLOCK, BlockType.RAW_COPPER_BLOCK,
        BlockType.QUARTZ_BLOCK
      ).buildAndRegister();
    BlockTag.builder(LAVA_BENDABLE.key()).add(BlockType.LAVA, BlockType.MAGMA_BLOCK).buildAndRegister();
    BlockTag.builder(MUD_BENDABLE.key())
      .add(BlockType.SOUL_SAND, BlockType.SOUL_SOIL, BlockType.BROWN_TERRACOTTA).endsWith("MUD").buildAndRegister();
    BlockTag.builder(ALL.key())
      .add(EARTH_BENDABLE).add(SAND_BENDABLE).add(METAL_BENDABLE).add(LAVA_BENDABLE).add(MUD_BENDABLE)
      .buildAndRegister();

    ItemTag.builder(METAL_KEYS.key()).add(Item.IRON_INGOT, Item.GOLD_INGOT, Item.COPPER_INGOT, Item.NETHERITE_INGOT)
      .buildAndRegister();
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
