/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.fabric.platform;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import me.moros.bending.api.util.data.DataContainer;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import me.moros.math.Position;
import me.moros.math.Vector3i;
import net.kyori.adventure.key.Key;
import net.minecraft.world.entity.Entity;

public enum FabricMetadata {
  INSTANCE;

  private final Map<Key, Pair<Position, UUID>> worlds = new ConcurrentHashMap<>();

  private Pair<Position, UUID> worldData(Key key) {
    return worlds.computeIfAbsent(key, u -> new Pair<>(new ConcurrentHashMap<>(), new ConcurrentHashMap<>()));
  }

  private DataHolder blockData(Map<Position, DataContainer> dataMap, int x, int y, int z) {
    return dataMap.computeIfAbsent(Vector3i.of(x, y, z), p -> DataContainer.simple());
  }

  private DataHolder entityData(Map<UUID, DataContainer> dataMap, UUID uuid) {
    return dataMap.computeIfAbsent(uuid, id -> DataContainer.simple());
  }

  public DataHolder metadata(Key world, int x, int y, int z) {
    return blockData(worldData(world).map1(), x, y, z);
  }

  public DataHolder metadata(Key world, UUID uuid) {
    return entityData(worldData(world).map2(), uuid);
  }

  public DataHolder metadata(Entity entity) {
    return metadata(entity.level().dimension().identifier(), entity.getUUID());
  }

  public boolean has(Key world, int x, int y, int z, DataKey<?> key) {
    return has(world, key, p -> p.map1().get(Vector3i.of(x, y, z)));
  }

  public boolean has(Entity entity, DataKey<?> key) {
    return has(entity.level().dimension().identifier(), key, p -> p.map2().get(entity.getUUID()));
  }

  private boolean has(Key worldKey, DataKey<?> key, Function<Pair<Position, UUID>, DataHolder> mapper) {
    var pair = worlds.get(worldKey);
    if (pair == null) {
      return false;
    }
    var dataHolder = mapper.apply(pair);
    return dataHolder != null && dataHolder.has(key);
  }

  // Cleanup empty data containers to avoid memory leaks
  public void removeEmpty() {
    worlds.entrySet().removeIf(e -> e.getValue().cleanup());
  }

  public void cleanup() {
    worlds.clear();
  }

  public void cleanup(Key world) {
    worlds.remove(world);
  }

  private record Pair<V1, V2>(Map<V1, DataContainer> map1, Map<V2, DataContainer> map2) {
    private boolean cleanup() {
      map1().entrySet().removeIf(e -> e.getValue().isEmpty());
      map2().entrySet().removeIf(e -> e.getValue().isEmpty());
      return map1.isEmpty() && map2.isEmpty();
    }
  }
}
