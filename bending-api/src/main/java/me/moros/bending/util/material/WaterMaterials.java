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
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;

/**
 * Group and categorize all water bendable materials.
 */
public final class WaterMaterials {
  public static final MaterialSetTag PLANT_BENDABLE;
  public static final MaterialSetTag ICE_BENDABLE;
  public static final MaterialSetTag SNOW_BENDABLE;
  public static final MaterialSetTag FULL_SOURCES;
  private static final MaterialSetTag ALL;

  static {
    NamespacedKey key = Metadata.NSK_MATERIAL;
    PLANT_BENDABLE = new MaterialSetTag(key)
      .add(Material.CACTUS, Material.MELON, Material.VINE)
      .add(Tag.FLOWERS.getValues())
      .add(Tag.SAPLINGS.getValues())
      .add(Tag.CROPS.getValues())
      .add(Tag.LEAVES.getValues())
      .add(MaterialTags.MUSHROOMS.getValues())
      .add(MaterialTags.MUSHROOM_BLOCKS.getValues())
      .add(MaterialTags.PUMPKINS.getValues())
      .lock();

    ICE_BENDABLE = new MaterialSetTag(key).add(Tag.ICE.getValues()).lock();

    SNOW_BENDABLE = new MaterialSetTag(key).add(Material.SNOW, Material.SNOW_BLOCK).lock();

    FULL_SOURCES = new MaterialSetTag(key)
      .add(Material.WATER, Material.CACTUS, Material.MELON, Material.SNOW_BLOCK)
      .add(ICE_BENDABLE.getValues())
      .add(Tag.LEAVES.getValues())
      .add(MaterialTags.MUSHROOM_BLOCKS.getValues())
      .add(MaterialTags.PUMPKINS.getValues())
      .lock();

    ALL = new MaterialSetTag(key)
      .add(PLANT_BENDABLE.getValues())
      .add(ICE_BENDABLE.getValues())
      .add(SNOW_BENDABLE.getValues())
      .add(Material.WATER).lock();
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
