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

package me.moros.bending.api.adapter;

import java.util.UUID;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.entity.display.Display;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;

/**
 * Interface for packet utilities and adapter.
 */
public interface PacketUtil {
  /**
   * Create a toast notification.
   * @param item the material to use in the icon for the notification
   * @param title the content of the notification
   * @return the constructed packet
   */
  default ClientboundPacket createNotification(Item item, Component title) {
    return DummyPacket.INSTANCE;
  }

  /**
   * Create a block update packet packet.
   * @param position the fake block's position
   * @param state the fake block's data
   * @return the constructed packet
   */
  default ClientboundPacket fakeBlock(Position position, BlockState state) {
    return DummyPacket.INSTANCE;
  }

  /**
   * Create a block break animation packet.
   * @param position the break animation's block position
   * @param progress the animation <a href="https://wiki.vg/Protocol#Set_Block_Destroy_Stage">stage</a>
   * @return the constructed packet
   */
  default ClientboundPacket fakeBreak(Position position, byte progress) {
    return DummyPacket.INSTANCE;
  }

  /**
   * Create a packet falling block entity.
   * @param center the spawn location
   * @param state the data to use for the falling block
   * @param velocity the initial velocity for the entity
   * @param gravity whether the entity will have gravity enabled
   * @return the constructed packet
   */
  default ClientboundPacket createFallingBlock(Position center, BlockState state, Vector3d velocity, boolean gravity) {
    return DummyPacket.INSTANCE;
  }

  /**
   * Create a packet display entity.
   * @param center the spawn location
   * @param properties the properties to use for the display entity
   * @return the constructed packet
   */
  default ClientboundPacket createDisplayEntity(Position center, Display<?> properties) {
    return DummyPacket.INSTANCE;
  }

  /**
   * Update a packet display entity's position.
   * @param id the display entity's id
   * @param position the display entity's new position
   * @return the constructed packet
   */
  default ClientboundPacket updateDisplayPosition(int id, Vector3d position) {
    return DummyPacket.INSTANCE;
  }

  /**
   * Remove the specified packet entity.
   * @param id a packet entity's unique id
   * @see #destroy(int[])
   */
  default void destroy(int id) {
    destroy(new int[]{id});
  }

  /**
   * Remove all specified packet entities.
   * @param ids an array of packet entities' unique ids
   */
  default void destroy(int[] ids) {
  }

  interface ClientboundPacket {
    int id();

    void send(Iterable<UUID> playerUUIDs);

    default void broadcast(World world, Position center) {
      broadcast(world, center, world.viewDistance() << 4);
    }

    void broadcast(World world, Position center, int dist);
  }
}
