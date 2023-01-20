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
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.platform.sound.SoundGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;

public final class BlockInitializer implements Initializer {
  @Override
  public void init() {
    for (var mat : BuiltInRegistries.BLOCK) {
      var key = BuiltInRegistries.BLOCK.getKey(mat);
      var type = BlockTypeImpl.getOrCreate(key);
      var data = mat.defaultBlockState();
      BlockTypeImpl.STATE_REGISTRY.register(PlatformAdapter.fromFabricData(data));
      BlockTypeImpl.PROPERTY_REGISTRY.register(mapProperties(type, data));
      if (mat.asItem() instanceof BlockItem item) {
        BlockTypeImpl.ITEM_REGISTRY.register(PlatformAdapter.fromFabricItem(item));
      }
    }
  }

  private BlockProperties mapProperties(BlockType type, net.minecraft.world.level.block.state.BlockState data) {
    var mat = data.getMaterial();
    return BlockProperties.builder(type, data.getBlock().getDescriptionId())
      .isAir(data.isAir())
      .isSolid(mat.isSolid())
      .isLiquid(mat.isLiquid())
      .isFlammable(mat.isFlammable())
      .hasGravity(data.getBlock() instanceof FallingBlock)
      .isCollidable(mat.blocksMotion())
      .hardness(data.getBlock().defaultDestroyTime())
      .soundGroup(mapSoundGroup(data.getSoundType())).build();
  }

  private SoundGroup mapSoundGroup(SoundType type) {
    return new SoundGroup(mapSound(type.getBreakSound()),
      mapSound(type.getStepSound()),
      mapSound(type.getPlaceSound()),
      mapSound(type.getHitSound()),
      mapSound(type.getFallSound())
    );
  }

  private Sound mapSound(SoundEvent sound) {
    //noinspection DataFlowIssue
    return Sound.registry().get(sound.getLocation()); // Defaulted registry
  }
}
