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

package me.moros.bending.fabric.platform;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.api.util.data.DataContainer;
import me.moros.bending.api.util.data.DataHolder;
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

  private DataHolder blockData(Map<Position, DataHolder> dataMap, int x, int y, int z) {
    return dataMap.computeIfAbsent(Vector3i.of(x, y, z), p -> DataContainer.simple());
  }

  private DataHolder entityData(Map<UUID, DataHolder> dataMap, UUID uuid) {
    return dataMap.computeIfAbsent(uuid, id -> DataContainer.simple());
  }

  public DataHolder metadata(Key world, int x, int y, int z) {
    return blockData(worldData(world).map1(), x, y, z);
  }

  public DataHolder metadata(Key world, UUID uuid) {
    return entityData(worldData(world).map2(), uuid);
  }

  public DataHolder metadata(Entity entity) {
    return metadata(entity.getLevel().dimension().location(), entity.getUUID());
  }

  public void cleanup() {
    worlds.clear();
  }

  public void cleanup(Key world) {
    worlds.remove(world);
  }

  private record Pair<V1, V2>(Map<V1, DataHolder> map1, Map<V2, DataHolder> map2) {
  }
}
