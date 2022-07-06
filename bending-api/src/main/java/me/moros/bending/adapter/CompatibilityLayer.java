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
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.RayTraceResult;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface CompatibilityLayer {
  @NonNull PacketAdapter packetAdapter();

  default boolean isNative() {
    return false;
  }

  default boolean setBlockFast(@NonNull Block block, @NonNull BlockData data) {
    block.setBlockData(data, false);
    return true;
  }

  private Block eyeBlock(Entity entity) {
    int x = entity.getLocation().getBlockX();
    int y = FastMath.floor(entity.getLocation().getY() + (entity.getHeight() * 0.85) - 0.11);
    int z = entity.getLocation().getBlockZ();
    return entity.getWorld().getBlockAt(x, y, z);
  }

  default boolean eyeInWater(@NonNull Entity entity) {
    return MaterialUtil.isWater(eyeBlock(entity));
  }

  default boolean eyeInLava(@NonNull Entity entity) {
    return MaterialUtil.isLava(eyeBlock(entity));
  }

  default @NonNull CompositeRayTrace rayTraceBlocks(@NonNull RayTraceContext context, @NonNull World world) {
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
}
