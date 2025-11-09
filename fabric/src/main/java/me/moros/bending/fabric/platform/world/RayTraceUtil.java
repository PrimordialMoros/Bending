/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.fabric.platform.world;

import java.util.function.Function;

import me.moros.bending.api.collision.raytrace.BlockRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.collision.raytrace.RayTrace;
import me.moros.bending.api.platform.world.World;
import me.moros.math.Vector3d;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

final class RayTraceUtil {
  private RayTraceUtil() {
  }

  static BlockRayTrace rayTraceBlocks(Context context, ServerLevel level, World world) {
    var s = context.origin();
    var e = context.endPoint();
    Vec3 startPos = new Vec3(s.x(), s.y(), s.z());
    Vec3 endPos = new Vec3(e.x(), e.y(), e.z());

    Block ccb = context.ignorePassable() ? Block.COLLIDER : Block.OUTLINE;
    Fluid ccf = context.ignoreLiquids() ? Fluid.NONE : Fluid.ANY;
    ClipContext clipContext = new ClipContext(context, startPos, endPos, ccb, ccf);

    return traverseBlocks(level, clipContext, RayTrace.miss(e), hitFactory(world));
  }

  private static BlockRayTrace traverseBlocks(ServerLevel level, ClipContext context, BlockRayTrace miss, Function<BlockHitResult, BlockRayTrace> hitFactory) {
    Vec3 start = context.getFrom();
    Vec3 end = context.getTo();
    if (start.equals(end)) {
      return miss;
    } else {
      double d0 = Mth.lerp(-1.0E-7D, end.x, start.x);
      double d1 = Mth.lerp(-1.0E-7D, end.y, start.y);
      double d2 = Mth.lerp(-1.0E-7D, end.z, start.z);
      double d3 = Mth.lerp(-1.0E-7D, start.x, end.x);
      double d4 = Mth.lerp(-1.0E-7D, start.y, end.y);
      double d5 = Mth.lerp(-1.0E-7D, start.z, end.z);
      int i = Mth.floor(d3);
      int j = Mth.floor(d4);
      int k = Mth.floor(d5);
      BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos(i, j, k);
      BlockRayTrace t0 = checkBlockCollision(level, context, mutableBlockPos, miss, hitFactory);
      if (t0 != null) {
        return t0;
      } else {
        double d6 = d0 - d3;
        double d7 = d1 - d4;
        double d8 = d2 - d5;
        int l = Mth.sign(d6);
        int i1 = Mth.sign(d7);
        int j1 = Mth.sign(d8);
        double d9 = l == 0 ? Double.MAX_VALUE : l / d6;
        double d10 = i1 == 0 ? Double.MAX_VALUE : i1 / d7;
        double d11 = j1 == 0 ? Double.MAX_VALUE : j1 / d8;
        double d12 = d9 * (l > 0 ? 1.0D - Mth.frac(d3) : Mth.frac(d3));
        double d13 = d10 * (i1 > 0 ? 1.0D - Mth.frac(d4) : Mth.frac(d4));
        double d14 = d11 * (j1 > 0 ? 1.0D - Mth.frac(d5) : Mth.frac(d5));
        BlockRayTrace result;
        do {
          if (d12 > 1.0D && d13 > 1.0D && d14 > 1.0D) {
            return miss;
          }
          if (d12 < d13) {
            if (d12 < d14) {
              i += l;
              d12 += d9;
            } else {
              k += j1;
              d14 += d11;
            }
          } else if (d13 < d14) {
            j += i1;
            d13 += d10;
          } else {
            k += j1;
            d14 += d11;
          }
          result = checkBlockCollision(level, context, mutableBlockPos.set(i, j, k), miss, hitFactory);
        } while (result == null);
        return result;
      }
    }
  }

  private static @Nullable BlockRayTrace checkBlockCollision(ServerLevel world, ClipContext context, BlockPos pos, BlockRayTrace miss, Function<BlockHitResult, BlockRayTrace> hitFactory) {
    if (context.ignore(pos.getX(), pos.getY(), pos.getZ())) {
      return null;
    }
    if (!world.isLoaded(pos)) {
      return miss;
    }
    BlockState iblockdata = world.getBlockState(pos);
    if (iblockdata.isAir()) {
      return null;
    }
    FluidState fluid = iblockdata.getFluidState();
    Vec3 vec3d = context.getFrom();
    Vec3 vec3d1 = context.getTo();
    VoxelShape voxelshape = context.getBlockShape(iblockdata, world, pos);
    BlockHitResult res0 = world.clipWithInteractionOverride(vec3d, vec3d1, pos, voxelshape, iblockdata);
    VoxelShape voxelshape1 = context.getFluidShape(fluid, world, pos);
    BlockHitResult res1 = voxelshape1.clip(vec3d, vec3d1, pos);
    double d0 = res0 == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(res0.getLocation());
    double d1 = res1 == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(res1.getLocation());
    return d0 <= d1 ? hitFactory.apply(res0) : hitFactory.apply(res1);
  }

  private static Function<BlockHitResult, @Nullable BlockRayTrace> hitFactory(World w) {
    return result -> {
      if (result == null) {
        return null;
      }
      var l = result.getLocation();
      var p = result.getBlockPos();
      var b = new me.moros.bending.api.platform.block.Block(w, p.getX(), p.getY(), p.getZ());
      return RayTrace.hit(Vector3d.of(l.x(), l.y(), l.z()), b);
    };
  }

  private record ClipContext(Context context, Vec3 getFrom, Vec3 getTo, Block block, Fluid fluid) {
    public VoxelShape getBlockShape(BlockState state, BlockGetter world, BlockPos pos) {
      return block.get(state, world, pos, CollisionContext.empty());
    }

    public VoxelShape getFluidShape(FluidState state, BlockGetter world, BlockPos pos) {
      return fluid.canPick(state) ? state.getShape(world, pos) : Shapes.empty();
    }

    public boolean ignore(int x, int y, int z) {
      return context.ignore(x, y, z);
    }
  }
}
