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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import org.spongepowered.api.ResourceKey;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.registry.RegistryTypes;

public final class BlockInitializer implements Initializer {
  @Override
  public void init() {
    var blockTypes = Sponge.game().registry(RegistryTypes.BLOCK_TYPE).stream().toList();
    for (var mat : blockTypes) {
      var key = PlatformAdapter.fromRsk(mat.key(RegistryTypes.BLOCK_TYPE));
      var type = BlockTypeImpl.getOrCreate(key);
      var data = mat.defaultState();
      BlockTypeImpl.STATE_REGISTRY.register(PlatformAdapter.fromSpongeData(data));
      BlockTypeImpl.PROPERTY_REGISTRY.register(mapProperties(type, data));
      mat.item().map(PlatformAdapter::fromSpongeItem).ifPresent(BlockTypeImpl.ITEM_REGISTRY::register);
    }
  }

  private BlockProperties mapProperties(BlockType type, BlockState data) {
    var nms = (net.minecraft.world.level.block.state.BlockState) data;
    var mat = nms.getMaterial();
    return BlockProperties.builder(type, nms.getBlock().getDescriptionId())
      .isAir(nms.isAir())
      .isSolid(mat.isSolid())
      .isLiquid(mat.isLiquid())
      .isFlammable(mat.isFlammable())
      .hasGravity(data.getOrElse(Keys.IS_GRAVITY_AFFECTED, false))
      .isCollidable(mat.blocksMotion())
      .hardness(nms.getBlock().defaultDestroyTime())
      .soundGroup(mapSoundGroup(nms.getSoundType())).build();
  }

  private SoundGroup mapSoundGroup(SoundType group) {
    return new SoundGroup(mapSound(group.getBreakSound()),
      mapSound(group.getStepSound()),
      mapSound(group.getPlaceSound()),
      mapSound(group.getHitSound()),
      mapSound(group.getFallSound())
    );
  }

  private Sound mapSound(SoundEvent sound) {
    Object key = sound.getLocation();
    //noinspection DataFlowIssue
    return Sound.registry().get(PlatformAdapter.fromRsk((ResourceKey) key)); // Defaulted registry
  }
}
