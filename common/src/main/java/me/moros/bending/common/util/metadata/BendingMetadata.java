/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.common.util.metadata;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.api.util.data.DataContainer;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import me.moros.math.Position;
import me.moros.math.Vector3i;
import net.kyori.adventure.key.Key;

public enum BendingMetadata {
  INSTANCE;

  private final MetadataHolder<UUID> entityMetadata = new MetadataHolder<>(new ConcurrentHashMap<>());
  private final Map<Key, MetadataHolder<Position>> worldMetadata = new ConcurrentHashMap<>();

  private MetadataHolder<Position> worldData(Key key) {
    return worldMetadata.computeIfAbsent(key, u -> new MetadataHolder<>(new ConcurrentHashMap<>()));
  }

  private DataHolder blockData(Map<Position, DataContainer> dataMap, int x, int y, int z) {
    return dataMap.computeIfAbsent(Vector3i.of(x, y, z), p -> DataContainer.simple());
  }

  private DataHolder entityData(UUID uuid) {
    return entityMetadata.data().computeIfAbsent(uuid, id -> DataContainer.simple());
  }

  public DataHolder metadata(Key world, int x, int y, int z) {
    return blockData(worldData(world).data(), x, y, z);
  }

  public DataHolder metadata(UUID uuid) {
    return entityData(uuid);
  }

  public boolean has(Key world, int x, int y, int z, DataKey<?> key) {
    var blockMetadata = worldMetadata.get(world);
    if (blockMetadata == null) {
      return false;
    }
    var dataHolder = blockMetadata.data().get(Vector3i.of(x, y, z));
    return dataHolder != null && dataHolder.has(key);
  }

  public boolean has(UUID uuid, DataKey<?> key) {
    var dataHolder = entityMetadata.data().get(uuid);
    return dataHolder != null && dataHolder.has(key);
  }

  // Cleanup empty data containers to avoid memory leaks
  public void removeEmpty() {
    entityMetadata.cleanup();
    worldMetadata.entrySet().removeIf(e -> e.getValue().cleanup());
  }

  public void clear() {
    removeEmpty();
    entityMetadata.data().clear();
    worldMetadata.clear();
  }

  public void cleanup(UUID uuid) {
    entityMetadata.data().remove(uuid);
  }

  public void cleanup(Key world) {
    worldMetadata.remove(world);
  }

  private record MetadataHolder<T>(Map<T, DataContainer> data) {
    private boolean cleanup() {
      data().entrySet().removeIf(e -> e.getValue().isEmpty());
      return data().isEmpty();
    }
  }
}
