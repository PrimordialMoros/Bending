/*
 * Copyright 2020-2022 Moros
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

import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.raytrace.CompositeRayTrace;
import me.moros.bending.model.raytrace.RayTraceContext;
import me.moros.bending.util.material.MaterialUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

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
   * @return whether a native adapter has been registered
   */
  static boolean hasNativeSupport() {
    return Holder.INSTANCE != null;
  }

  /**
   * Attempt to use NMS to set BlockData for a specific block.
   * @param block the block to set
   * @param data the new block data
   * @return true if block was changed, false otherwise
   */
  default boolean setBlockFast(Block block, BlockData data) {
    block.setBlockData(data, false);
    return true;
  }

  private Block eyeBlock(Entity entity) {
    int x = entity.getLocation().getBlockX();
    int y = FastMath.floor(entity.getLocation().getY() + (entity.getHeight() * 0.85) - 0.11);
    int z = entity.getLocation().getBlockZ();
    return entity.getWorld().getBlockAt(x, y, z);
  }

  /**
   * Check if an entity is underwater.
   * @param entity the entity to check
   * @return if there is water fluid at the level of the entity's eyes.
   */
  default boolean eyeInWater(Entity entity) {
    return MaterialUtil.isWater(eyeBlock(entity));
  }

  /**
   * Check if an entity is under lava.
   * @param entity the entity to check
   * @return if there is lava fluid at the level of the entity's eyes.
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
  default CompositeRayTrace rayTraceBlocks(RayTraceContext context, World world) {
    Location start = context.start().toLocation(world);
    Vector3d dir = context.end().subtract(context.start());
    double range = dir.length();
    FluidCollisionMode mode = context.ignoreLiquids() ? FluidCollisionMode.NEVER : FluidCollisionMode.ALWAYS;
    RayTraceResult result = world.rayTraceBlocks(start, dir.toBukkitVector(), range, mode, context.ignorePassable());
    CompositeRayTrace missResult = CompositeRayTrace.miss(context.end());
    if (result == null) {
      return missResult;
    }
    Block block = result.getHitBlock();
    BlockFace face = result.getHitBlockFace();
    if (block == null || face == null || context.ignoreBlocks().contains(block)) {
      return missResult;
    }
    return CompositeRayTrace.hit(new Vector3d(result.getHitPosition()), block, face);
  }

  /**
   * Send a notification.
   * @param player the notification's receiver
   * @param material the material to use in the icon for the notification
   * @param title the content of the notification
   */
  default void sendNotification(Player player, Material material, Component title) {
    Times times = Times.times(Ticks.duration(10), Ticks.duration(70), Ticks.duration(10));
    player.showTitle(Title.title(title, Component.empty(), times));
  }

  /**
   * Create a packet armor stand entity.
   * @param world the world for the packet entity to appear in
   * @param center the spawn location
   * @param material the material to use for the armor stand's head equipment
   * @param velocity the initial velocity for the entity
   * @param gravity whether the entity will have gravity enabled
   * @return the packet entity unique id or 0 if not supported
   */
  default int createArmorStand(World world, Vector3d center, Material material, Vector3d velocity, boolean gravity) {
    return 0;
  }

  /**
   * Create a packet falling block entity.
   * @param world the world for the packet entity to appear in
   * @param center the spawn location
   * @param data the data to use for the falling block
   * @param velocity the initial velocity for the entity
   * @param gravity whether the entity will have gravity enabled
   * @return the packet entity unique id or 0 if not supported
   */
  default int createFallingBlock(World world, Vector3d center, BlockData data, Vector3d velocity, boolean gravity) {
    return 0;
  }

  /**
   * Send a block update packet to every connection within view distance.
   * @param world the world for the fake block to appear in
   * @param center the fake block's location
   * @param data the fake block's data
   */
  default void fakeBlock(World world, Vector3d center, BlockData data) {
  }

  /**
   * Send a block break animation to every connection within view distance.
   * @param world the world for the animation to appear in
   * @param center the block location for the animation
   * @param progress the animation <a href="https://wiki.vg/Protocol#Set_Block_Destroy_Stage">stage</a>
   */
  default void fakeBreak(World world, Vector3d center, byte progress) {
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
