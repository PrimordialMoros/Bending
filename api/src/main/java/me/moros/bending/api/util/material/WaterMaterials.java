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
import me.moros.bending.api.util.KeyUtil;

/**
 * Group and categorize all water bendable materials.
 */
public final class WaterMaterials {
  public static final BlockTag PLANT_BENDABLE = BlockTag.reference(KeyUtil.simple("plant_sources"));
  public static final BlockTag ICE_BENDABLE = BlockTag.reference(KeyUtil.simple("ice_sources"));
  public static final BlockTag SNOW_BENDABLE = BlockTag.reference(KeyUtil.simple("snow_sources"));
  public static final BlockTag FULL_SOURCES = BlockTag.reference(KeyUtil.simple("full_water_sources"));
  private static final BlockTag ALL = BlockTag.reference(KeyUtil.simple("all_water_sources"));

  public static void init() {
    BlockTag.builder(PLANT_BENDABLE.key())
      .add(BlockTag.reference(KeyUtil.simple("plant_sources/extra")))
      .add(BlockTag.FLOWERS)
      .add(BlockTag.SAPLINGS)
      .add(BlockTag.CROPS)
      .add(BlockTag.LEAVES)
      .add(BlockType.CACTUS, BlockType.MELON, BlockType.VINE,
        BlockType.CARVED_PUMPKIN, BlockType.JACK_O_LANTERN, BlockType.PUMPKIN,
        BlockType.BROWN_MUSHROOM, BlockType.RED_MUSHROOM,
        BlockType.BROWN_MUSHROOM_BLOCK, BlockType.RED_MUSHROOM_BLOCK, BlockType.MUSHROOM_STEM)
      .buildAndRegister();

    BlockTag.builder(ICE_BENDABLE.key())
      .add(BlockTag.reference(KeyUtil.simple("ice_sources/extra")))
      .add(BlockTag.ICE)
      .buildAndRegister();

    BlockTag.builder(SNOW_BENDABLE.key())
      .add(BlockTag.reference(KeyUtil.simple("snow_sources/extra")))
      .add(BlockType.SNOW, BlockType.SNOW_BLOCK)
      .buildAndRegister();

    BlockTag.builder(FULL_SOURCES.key())
      .add(BlockTag.reference(KeyUtil.simple("full_water_sources/extra")))
      .add(ICE_BENDABLE)
      .add(BlockTag.LEAVES)
      .add(BlockType.WATER, BlockType.CACTUS, BlockType.MELON, BlockType.SNOW_BLOCK,
        BlockType.CARVED_PUMPKIN, BlockType.JACK_O_LANTERN, BlockType.PUMPKIN,
        BlockType.BROWN_MUSHROOM_BLOCK, BlockType.RED_MUSHROOM_BLOCK, BlockType.MUSHROOM_STEM)
      .buildAndRegister();

    BlockTag.builder(ALL.key())
      .add(BlockTag.reference(KeyUtil.simple("water_sources/extra")))
      .add(PLANT_BENDABLE).add(ICE_BENDABLE).add(SNOW_BENDABLE).add(BlockType.WATER)
      .buildAndRegister();
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
