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

package me.moros.bending.util.material;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import me.moros.bending.Bending;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public class WaterMaterials {
  public static final MaterialSetTag PLANT_BENDABLE;
  public static final MaterialSetTag ICE_BENDABLE;
  public static final MaterialSetTag ALL;

  static {
    NamespacedKey key = Bending.dataLayer().NSK_MATERIAL;
    PLANT_BENDABLE = new MaterialSetTag(key)
      .add(Material.DEAD_BUSH, Material.CACTUS, Material.MELON, Material.VINE)
      .add(Tag.FLOWERS.getValues())
      .add(Tag.SAPLINGS.getValues())
      .add(Tag.CROPS.getValues())
      .add(Tag.LEAVES.getValues())
      .add(MaterialTags.MUSHROOMS.getValues())
      .add(MaterialTags.MUSHROOM_BLOCKS.getValues())
      .add(MaterialTags.PUMPKINS.getValues())
      .ensureSize("Plants", 47);

    ICE_BENDABLE = new MaterialSetTag(key).add(Tag.ICE.getValues()).ensureSize("Ice", 4);

    ALL = new MaterialSetTag(key)
      .add(PLANT_BENDABLE.getValues())
      .add(ICE_BENDABLE.getValues())
      .add(Material.WATER, Material.SNOW, Material.SNOW_BLOCK).ensureSize("Waterbendable", 54);
  }

  public static boolean isWaterBendable(@NonNull Block block) {
    return ALL.isTagged(block);
  }

  public static boolean isIceBendable(@NonNull Block block) {
    return ICE_BENDABLE.isTagged(block);
  }

  public static boolean isWaterOrIceBendable(@NonNull Block block) {
    return block.getType() == Material.WATER || ICE_BENDABLE.isTagged(block);
  }

  public static boolean isPlantBendable(@NonNull Block block) {
    return PLANT_BENDABLE.isTagged(block);
  }
}
