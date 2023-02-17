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

package me.moros.bending.paper.adapter;

import me.moros.bending.api.adapter.NativeAdapter;
import me.moros.bending.api.collision.raytrace.BlockRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.collision.raytrace.RayTrace;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.paper.platform.world.BukkitWorld;
import me.moros.math.Vector3d;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class NativeAdapterImpl implements NativeAdapter {
  public static final NativeAdapter DUMMY = new NativeAdapterImpl();

  private NativeAdapterImpl() {
  }

  @Override
  public BlockRayTrace rayTraceBlocks(World world, Context context) {
    var handle = ((BukkitWorld) world).handle();
    var loc = new Location(handle, context.origin().x(), context.origin().y(), context.origin().z());
    var dir = new Vector(context.dir().x(), context.dir().y(), context.dir().z());
    var mode = context.ignoreLiquids() ? FluidCollisionMode.NEVER : FluidCollisionMode.ALWAYS;
    var result = handle.rayTraceBlocks(loc, dir, context.range(), mode, context.ignorePassable());
    if (result == null || result.getHitBlock() == null) {
      return RayTrace.miss(context.endPoint());
    }
    var pos = result.getHitPosition();
    Vector3d point = Vector3d.of(pos.getX(), pos.getY(), pos.getZ());
    Block block = world.blockAt(result.getHitBlock().getX(), result.getHitBlock().getY(), result.getHitBlock().getZ());
    return RayTrace.hit(point, block);
  }
}
