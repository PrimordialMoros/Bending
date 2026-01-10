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

package me.moros.bending.api.adapter;

import java.util.UUID;

import me.moros.bending.api.adapter.PacketUtil.ClientboundPacket;
import me.moros.bending.api.platform.world.World;
import me.moros.math.Position;

record DummyPacket() implements ClientboundPacket {
  static final ClientboundPacket INSTANCE = new DummyPacket();

  @Override
  public int id() {
    return 0;
  }

  @Override
  public void send(Iterable<UUID> playerUUIDs) {
  }

  @Override
  public void broadcast(World world, Position center, int dist) {
  }
}
