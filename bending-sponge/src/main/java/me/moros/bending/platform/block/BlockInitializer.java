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
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.effect.sound.SoundType;
import org.spongepowered.api.registry.RegistryTypes;

public final class BlockInitializer extends AbstractInitializer {
  public BlockInitializer(Path path, Logger logger) {
    super(path, logger);
  }

  @Override
  public void init() {
    var map = collect();
    Collection<Key> missing = new ArrayList<>();
    for (BlockTag tag : BlockTag.registry()) {
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
      var mat = PlatformAdapter.BLOCK_MATERIAL_INDEX.key(type);
      if (mat != null) {
        var data = mat.defaultState();
        BlockTypeImpl.STATE_REGISTRY.register(PlatformAdapter.fromSpongeData(data));
        BlockTypeImpl.PROPERTY_REGISTRY.register(mapProperties(type, data));
        mat.item().map(PlatformAdapter.ITEM_MATERIAL_INDEX::value).ifPresent(BlockTypeImpl.ITEM_REGISTRY::register);
      }
    }
  }

  private Map<Key, Set<BlockType>> collect() {
    Map<Key, Set<BlockType>> map = new HashMap<>();
    var spongeRegistry = Sponge.game().registry(RegistryTypes.BLOCK_TYPE);
    var it = spongeRegistry.tags().iterator();
    for (var tag = it.next(); it.hasNext(); ) {
      Set<BlockType> data = spongeRegistry.taggedValues(tag).stream().map(PlatformAdapter.BLOCK_MATERIAL_INDEX::value)
        .filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet());
      map.put(PlatformAdapter.fromRsk(tag.key()), data);
    }
    return map;
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
      .soundGroup(mapSoundGroup(data)).build();
  }

  private SoundGroup mapSoundGroup(BlockState data) {
    var bs = data.type().soundGroup();
    return new SoundGroup(mapSound(bs.breakSound()),
      mapSound(bs.stepSound()),
      mapSound(bs.placeSound()),
      mapSound(bs.hitSound()),
      mapSound(bs.fallSound())
    );
  }

  private Sound mapSound(SoundType sound) {
    return Objects.requireNonNull(Sound.registry().get(PlatformAdapter.fromRsk(sound.key())));
  }
}
