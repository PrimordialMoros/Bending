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

package me.moros.bending.util.material;

import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockTag;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.util.KeyUtil;

/**
 * Group and categorize all water bendable materials.
 */
public final class WaterMaterials {
  public static final BlockTag PLANT_BENDABLE = BlockTag.reference(KeyUtil.simple("plant-sources"));
  public static final BlockTag ICE_BENDABLE = BlockTag.reference(KeyUtil.simple("ice-sources"));
  public static final BlockTag SNOW_BENDABLE = BlockTag.reference(KeyUtil.simple("snow-sources"));
  public static final BlockTag FULL_SOURCES = BlockTag.reference(KeyUtil.simple("full-water-sources"));
  private static final BlockTag ALL = BlockTag.reference(KeyUtil.simple("all-water-sources"));

  public static void init() {
    BlockTag.builder(PLANT_BENDABLE.key())
      .add(BlockType.CACTUS, BlockType.MELON, BlockType.VINE)
      .add(BlockTag.FLOWERS)
      .add(BlockTag.SAPLINGS)
      .add(BlockTag.CROPS)
      .add(BlockTag.LEAVES)
      .add(BlockType.BROWN_MUSHROOM, BlockType.RED_MUSHROOM)
      .add(BlockType.BROWN_MUSHROOM_BLOCK, BlockType.RED_MUSHROOM_BLOCK, BlockType.MUSHROOM_STEM)
      .add(BlockType.CARVED_PUMPKIN, BlockType.JACK_O_LANTERN, BlockType.PUMPKIN)
      .buildAndRegister();
    BlockTag.builder(ICE_BENDABLE.key()).add(BlockTag.ICE).buildAndRegister();
    BlockTag.builder(SNOW_BENDABLE.key()).add(BlockType.SNOW, BlockType.SNOW_BLOCK).buildAndRegister();
    BlockTag.builder(FULL_SOURCES.key()).add(BlockType.WATER, BlockType.CACTUS, BlockType.MELON, BlockType.SNOW_BLOCK)
      .add(ICE_BENDABLE).add(BlockTag.LEAVES)
      .add(BlockType.BROWN_MUSHROOM_BLOCK, BlockType.RED_MUSHROOM_BLOCK, BlockType.MUSHROOM_STEM)
      .add(BlockType.CARVED_PUMPKIN, BlockType.JACK_O_LANTERN, BlockType.PUMPKIN).buildAndRegister();
    BlockTag.builder(ALL.key()).add(PLANT_BENDABLE).add(ICE_BENDABLE).add(SNOW_BENDABLE).add(BlockType.WATER).buildAndRegister();
  }

  private WaterMaterials() {
  }

  public static boolean isWaterBendable(Block block) {
    return MaterialUtil.isWater(block) || ALL.isTagged(block);
  }

  public static boolean isWaterNotPlant(Block block) {
    return isWaterBendable(block) && !PLANT_BENDABLE.isTagged(block);
  }

  public static boolean isIceBendable(Block block) {
    return ICE_BENDABLE.isTagged(block);
  }

  public static boolean isSnowBendable(Block block) {
    return SNOW_BENDABLE.isTagged(block);
  }

  public static boolean isWaterOrIceBendable(Block block) {
    return MaterialUtil.isWater(block) || ICE_BENDABLE.isTagged(block);
  }

  public static boolean isPlantBendable(Block block) {
    return PLANT_BENDABLE.isTagged(block);
  }

  public static boolean isFullWaterSource(Block block) {
    return FULL_SOURCES.isTagged(block) || MaterialUtil.isWaterPlant(block);
  }
}
