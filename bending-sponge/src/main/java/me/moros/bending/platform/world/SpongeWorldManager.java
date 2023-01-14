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

package me.moros.bending.platform.world;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.model.data.DataContainer;
import me.moros.bending.model.data.DataHolder;
import me.moros.math.Position;
import me.moros.math.Vector3i;

public enum SpongeWorldManager {
  INSTANCE;

  private final Map<UUID, Map<Position, DataHolder>> worlds = new ConcurrentHashMap<>();

  private Map<Position, DataHolder> worldData(UUID uuid) {
    return worlds.computeIfAbsent(uuid, u -> new ConcurrentHashMap<>());
  }

  private DataHolder blockData(Map<Position, DataHolder> worldData, int x, int y, int z) {
    return worldData.computeIfAbsent(Vector3i.of(x, y, z), p -> DataContainer.simple());
  }

  public DataHolder metadata(UUID world, int x, int y, int z) {
    return blockData(worldData(world), x, y, z);
  }

  public void cleanup() {
    worlds.clear();
  }

  public void cleanup(UUID world) {
    worlds.remove(world);
  }
}
