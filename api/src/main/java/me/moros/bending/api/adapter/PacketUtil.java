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

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.entity.display.Display;
import me.moros.bending.api.platform.entity.player.Player;
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
   * Send a notification.
   * @param player the notification's receiver
   * @param item the material to use in the icon for the notification
   * @param title the content of the notification
   */
  default void sendNotification(Player player, Item item, Component title) {
  }

  /**
   * Send a block update packet to every connection within view distance.
   * @param block the world-position tuple for the fake block to appear in
   * @param state the fake block's data
   */
  default void fakeBlock(Block block, BlockState state) {
  }

  /**
   * Send a block break animation to every connection within view distance.
   * @param block the world-position tuple for the animation to appear in
   * @param progress the animation <a href="https://wiki.vg/Protocol#Set_Block_Destroy_Stage">stage</a>
   */
  default void fakeBreak(Block block, byte progress) {
  }

  /**
   * Create a packet falling block entity.
   * @param world the world for the packet entity to appear in
   * @param center the spawn location
   * @param state the data to use for the falling block
   * @param velocity the initial velocity for the entity
   * @param gravity whether the entity will have gravity enabled
   * @return the packet entity unique id or 0 if not supported
   */
  default int createFallingBlock(World world, Position center, BlockState state, Vector3d velocity, boolean gravity) {
    return 0;
  }

  /**
   * Create a packet armor stand entity.
   * @param world the world for the packet entity to appear in
   * @param center the spawn location
   * @param item the item to use for the armor stand's head equipment
   * @param velocity the initial velocity for the entity
   * @param gravity whether the entity will have gravity enabled
   * @return the packet entity unique id or 0 if not supported
   */
  default int createArmorStand(World world, Position center, Item item, Vector3d velocity, boolean gravity) {
    return 0;
  }

  /**
   * Create a packet display entity.
   * @param world the world for the packet entity to appear in
   * @param center the spawn location
   * @param properties the properties to use for the display entity
   * @return the packet entity unique id or 0 if not supported
   */
  default int createDisplayEntity(World world, Position center, Display<?> properties) {
    return 0;
  }

  /**
   * Update a packet display entity's properties.
   * @param world the world the packet entity is in
   * @param center the center location to broadcast packets from
   * @param id the display entity's id
   * @param properties the display entity's new properties
   */
  default void updateDisplay(World world, Position center, int id, Display<?> properties) {
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
}
