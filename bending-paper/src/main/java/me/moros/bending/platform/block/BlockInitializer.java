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

package me.moros.bending.platform.block;

import me.moros.bending.platform.Initializer;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.platform.sound.SoundGroup;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.block.data.BlockData;

public final class BlockInitializer implements Initializer {
  @Override
  public void init() {
    for (Material mat : Registry.MATERIAL) {
      if (mat.isBlock()) {
        var key = PlatformAdapter.fromNsk(mat.getKey());
        var type = BlockTypeImpl.getOrCreate(key);
        var data = mat.createBlockData();
        BlockTypeImpl.STATE_REGISTRY.register(PlatformAdapter.fromBukkitData(data));
        BlockTypeImpl.PROPERTY_REGISTRY.register(mapProperties(type, data));
        var item = Item.registry().get(key);
        if (item != null) {
          BlockTypeImpl.ITEM_REGISTRY.register(item);
        }
      }
    }
  }

  private BlockProperties mapProperties(BlockType type, BlockData data) {
    var mat = data.getMaterial();
    return BlockProperties.builder(type, mat.translationKey())
      .isAir(mat.isAir())
      .isSolid(mat.isSolid())
      .isLiquid(type == BlockType.WATER || type == BlockType.LAVA || type == BlockType.BUBBLE_COLUMN)
      .isFlammable(mat.isFlammable())
      .hasGravity(mat.hasGravity())
      .isCollidable(mat.isCollidable())
      .hardness(mat.getHardness())
      .soundGroup(mapSoundGroup(data.getSoundGroup())).build();
  }

  private SoundGroup mapSoundGroup(org.bukkit.SoundGroup group) {
    return new SoundGroup(mapSound(group.getBreakSound()),
      mapSound(group.getStepSound()),
      mapSound(group.getPlaceSound()),
      mapSound(group.getHitSound()),
      mapSound(group.getFallSound())
    );
  }

  private Sound mapSound(org.bukkit.Sound sound) {
    //noinspection DataFlowIssue
    return Sound.registry().get(PlatformAdapter.fromNsk(sound.getKey())); // Defaulted registry
  }
}
