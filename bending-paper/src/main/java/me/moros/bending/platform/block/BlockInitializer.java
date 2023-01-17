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
import java.util.HashMap;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.data.BlockData;
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
    for (BlockType type : BlockType.registry()) {
      var mat = PlatformAdapter.BLOCK_MATERIAL_INDEX.value(type);
      if (mat != null) {
        var data = mat.createBlockData();
        BlockTypeImpl.STATE_REGISTRY.register(PlatformAdapter.fromBukkitData(data));
        BlockTypeImpl.PROPERTY_REGISTRY.register(mapProperties(type, data));
        var item = PlatformAdapter.ITEM_MATERIAL_INDEX.key(mat);
        if (item != null) {
          BlockTypeImpl.ITEM_REGISTRY.register(item);
        }
      }
    }
  }

  private Map<Key, Set<BlockType>> collect() {
    Map<Key, Set<BlockType>> map = new HashMap<>();
    for (var tag : Bukkit.getTags(Tag.REGISTRY_BLOCKS, Material.class)) {
      Set<BlockType> data = tag.getValues().stream().map(PlatformAdapter.BLOCK_MATERIAL_INDEX::key)
        .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
      map.put(PlatformAdapter.fromNsk(tag.getKey()), data);
    }
    return map;
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
    return Objects.requireNonNull(Sound.registry().get(PlatformAdapter.fromNsk(sound.getKey())));
  }
}
