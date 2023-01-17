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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import me.moros.bending.model.registry.Container;
import me.moros.bending.platform.AbstractInitializer;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.platform.sound.SoundGroup;
import net.kyori.adventure.key.Key;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet.Named;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import org.slf4j.Logger;

public final class BlockInitializer extends AbstractInitializer {
  public BlockInitializer(Path path, Logger logger) {
    super(path, logger);
  }

  @Override
  public void init() {
    var map = collect();
    Collection<Key> missing = new ArrayList<>();
    for (var tag : BlockTag.registry()) {
      Key key = tag.key();
      var data = map.get(key);
      if (data != null && !data.isEmpty()) {
        TagImpl.DATA_REGISTRY.register(Container.create(key, data));
      } else {
        missing.add(key);
      }
    }
    checkMissing("blocktags.log", "Missing block tags: %d", missing);
    for (var type : BlockType.registry()) {
      var mat = PlatformAdapter.BLOCK_MATERIAL_INDEX.key(type);
      if (mat != null) {
        var data = mat.defaultBlockState();
        BlockTypeImpl.STATE_REGISTRY.register(PlatformAdapter.fromFabricData(data));
        BlockTypeImpl.PROPERTY_REGISTRY.register(mapProperties(type, data));
        if (mat.asItem() instanceof BlockItem item) {
          BlockTypeImpl.ITEM_REGISTRY.register(PlatformAdapter.ITEM_MATERIAL_INDEX.value(item));
        }
      }
    }
  }

  private Map<Key, Set<BlockType>> collect() {
    return BuiltInRegistries.BLOCK.getTags()
      .collect(Collectors.toMap(p -> p.getFirst().location(), p -> toSet(p.getSecond())));
  }

  private Set<BlockType> toSet(Named<Block> holder) {
    return holder.stream().map(Holder::value).map(PlatformAdapter.BLOCK_MATERIAL_INDEX::value)
      .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
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
    return Objects.requireNonNull(Sound.registry().get(sound.getLocation()));
  }
}
