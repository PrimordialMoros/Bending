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

package me.moros.bending.adapter;

import me.moros.bending.model.raytrace.BlockRayTrace;
import me.moros.bending.model.raytrace.Context;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.player.Player;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.world.World;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;

/**
 * Interface for all NMS and Packet shenanigans that Bending takes advantage of.
 */
public interface NativeAdapter {
  /**
   * Disregard, internal use only
   */
  final class Holder {
    private static final NativeAdapter DUMMY = new NativeAdapter() {
    };
    private static NativeAdapter INSTANCE;

    private Holder() {
    }
  }

  /**
   * Try to add a custom NMS adapter.
   * @param adapter the adapter to inject
   */
  static void inject(NativeAdapter adapter) {
    if (Holder.INSTANCE != null) {
      throw new IllegalStateException("NativeAdapter has already been initialized!");
    }
    if (adapter == Holder.DUMMY) {
      throw new IllegalArgumentException("Injected NativeAdapter is not native!");
    }
    Holder.INSTANCE = adapter;
  }

  /**
   * Retrieve a native adapter if one was injected.
   * If no adapter has been registered then it will fall back to a default adapter for compatibility.
   * The default adapter might offer less accuracy and worse performance. Moreover, some features may not work at all.
   * @return the current NMS adapter
   * @see #hasNativeSupport()
   */
  static NativeAdapter instance() {
    return hasNativeSupport() ? Holder.INSTANCE : Holder.DUMMY;
  }

  /**
   * Check if a valid native adapter has been registered.
   * @return the result
   */
  static boolean hasNativeSupport() {
    return Holder.INSTANCE != null;
  }

  /**
   * Attempt to use NMS to set BlockData for a specific block.
   * @param block the block to set
   * @param state the new block data
   * @return true if block was changed, false otherwise
   */
  default boolean setBlockFast(Block block, BlockState state) {
    block.world().setBlockState(block, state);
    return true;
  }

  private Block eyeBlock(Entity entity) {
    Vector3d loc = entity.location();
    int x = loc.blockX();
    int y = FastMath.floor(loc.y() + (entity.height() * 0.85) - 0.11);
    int z = loc.blockZ();
    return entity.world().blockAt(x, y, z);
  }

  /**
   * Check if an entity is underwater.
   * @param entity the entity to check
   * @return if there is water fluid at the level of the entity's eyes
   */
  default boolean eyeInWater(Entity entity) {
    return MaterialUtil.isWater(eyeBlock(entity));
  }

  /**
   * Check if an entity is under lava.
   * @param entity the entity to check
   * @return if there is lava fluid at the level of the entity's eyes
   */
  default boolean eyeInLava(Entity entity) {
    return MaterialUtil.isLava(eyeBlock(entity));
  }

  /**
   * Perform a raytrace for blocks and return the result.
   * @param context the raytrace context
   * @param world the world to perform the raytrace in
   * @return the result of the performed raytrace
   */
  default BlockRayTrace rayTraceBlocks(Context context, World world) {
    return world.rayTraceBlocks(context);
  }

  /**
   * Send a notification.
   * @param player the notification's receiver
   * @param item the material to use in the icon for the notification
   * @param title the content of the notification
   */
  default void sendNotification(Player player, Item item, Component title) {
    Times times = Times.times(Ticks.duration(10), Ticks.duration(70), Ticks.duration(10));
    player.showTitle(Title.title(title, Component.empty(), times));
  }

  /**
   * Create a packet armor stand entity.
   * @param world the world for the packet entity to appear in
   * @param center the spawn location
   * @param item the material to use for the armor stand's head equipment
   * @param velocity the initial velocity for the entity
   * @param gravity whether the entity will have gravity enabled
   * @return the packet entity unique id or 0 if not supported
   */
  default int createArmorStand(World world, Position center, Item item, Vector3d velocity, boolean gravity) {
    return 0;
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

  /**
   * Try to power a block if it's a lightning rod.
   * @param block the block to power
   * @return true if a lightning rod was powered, false otherwise
   */
  default boolean tryPowerLightningRod(Block block) { // Only native implementation handles continuous lightning strikes
    return false;
  }
}
