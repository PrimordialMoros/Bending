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

package me.moros.bending.util.internal;

import java.util.Set;
import java.util.stream.Collectors;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.RayTrace.CompositeResult;
import me.moros.bending.util.collision.AABBUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftEntity;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class NMSUtil {
  private NMSUtil() {
  }

  public static boolean setBlockFast(@NonNull Block block, @NonNull BlockData data) {
    BlockPos position = new BlockPos(block.getX(), block.getY(), block.getZ());
    return ((CraftWorld) block.getWorld()).getHandle().setBlock(position, ((CraftBlockData) data).getState(), 2);
  }

  public static boolean eyeInWater(@NonNull Entity entity) {
    return ((CraftEntity) entity).getHandle().isEyeInFluid(FluidTags.WATER);
  }

  public static boolean eyeInLava(@NonNull Entity entity) {
    return ((CraftEntity) entity).getHandle().isEyeInFluid(FluidTags.LAVA);
  }

  public static AABB dimensions(@NonNull Entity entity, @NonNull Vector3d point) {
    net.minecraft.world.phys.AABB b = ((CraftEntity) entity).getHandle().getBoundingBoxAt(point.x(), point.y(), point.z());
    return new AABB(new Vector3d(b.minX, b.minY, b.minZ), new Vector3d(b.maxX, b.maxY, b.maxZ));
  }

  public static AABB dimensions(@NonNull Block block, @NonNull Vector3d point) {
    CraftBlock cb = (CraftBlock) block;
    VoxelShape shape = cb.getNMS().getShape(cb.getHandle(), cb.getPosition());
    if (shape.isEmpty() || !cb.getNMS().getBlock().hasCollision) {
      return AABBUtil.DUMMY_COLLIDER;
    }
    net.minecraft.world.phys.AABB b = shape.bounds();
    Vector3d min = new Vector3d(b.minX + point.x(), b.minY + point.y(), b.minZ + point.z());
    Vector3d max = new Vector3d(b.maxX + point.x(), b.maxY + point.y(), b.maxZ + point.z());
    return new AABB(min, max);
  }

  public static @NonNull CompositeResult rayTraceBlocks(@NonNull RayTrace rt, @NonNull World world) {
    Vector3d s = rt.origin();
    Vector3d e = rt.end();
    Vec3 startPos = new Vec3(s.x(), s.y(), s.z());
    Vec3 endPos = new Vec3(e.x(), e.y(), e.z());

    ClipContext.Block ccb = rt.ignorePassable() ? ClipContext.Block.COLLIDER : ClipContext.Block.OUTLINE;
    ClipContext.Fluid ccf = rt.ignoreLiquids() ? Fluid.NONE : Fluid.ANY;
    ClipContext clipContext = new ClipContext(startPos, endPos, ccb, ccf, null);

    BlockHit miss = new BlockHit(clipContext.getTo(), new BlockPos(clipContext.getTo()));
    Set<BlockPos> ignored = rt.ignored().stream()
      .map(b -> new BlockPos(b.getX(), b.getY(), b.getZ())).collect(Collectors.toSet());

    BlockHit result = traverseBlocks(world, clipContext, ignored, miss);
    if (result != null && !miss.equals(result)) {
      Vector3i bp = result.blockPosition();
      Block block = world.getBlockAt(bp.x(), bp.y(), bp.z());
      return new CompositeResult(result.position(), block, null);
    }
    return new CompositeResult(miss.position(), null, null);
  }

  private static BlockHit traverseBlocks(World world, ClipContext context, Set<BlockPos> ignored, BlockHit miss) {
    Vec3 start = context.getFrom();
    Vec3 end = context.getTo();
    Level level = ((CraftWorld) world).getHandle();
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
      BlockHit t0 = checkBlockCollision(level, context, ignored, mutableBlockPos, miss);
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
        BlockHit result;
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
          result = checkBlockCollision(level, context, ignored, mutableBlockPos.set(i, j, k), miss);
        } while (result == null);
        return result;
      }
    }
  }

  private static BlockHit checkBlockCollision(Level world, ClipContext context, Set<BlockPos> ignored, BlockPos pos, BlockHit miss) {
    if (ignored.contains(pos)) {
      return null;
    }
    BlockState iblockdata = world.getBlockStateIfLoaded(pos);
    if (iblockdata == null) {
      return miss;
    }
    if (iblockdata.isAir()) return null;
    FluidState fluid = iblockdata.getFluidState();
    Vec3 vec3d = context.getFrom();
    Vec3 vec3d1 = context.getTo();
    VoxelShape voxelshape = context.getBlockShape(iblockdata, world, pos);
    BlockHitResult res0 = world.clipWithInteractionOverride(vec3d, vec3d1, pos, voxelshape, iblockdata);
    VoxelShape voxelshape1 = context.getFluidShape(fluid, world, pos);
    BlockHitResult res1 = voxelshape1.clip(vec3d, vec3d1, pos);
    double d0 = res0 == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(res0.getLocation());
    double d1 = res1 == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(res1.getLocation());
    BlockHit b0 = res0 == null ? null : new BlockHit(res0);
    BlockHit b1 = res1 == null ? null : new BlockHit(res1);
    return d0 <= d1 ? b0 : b1;
  }

  private record BlockHit(Vector3d position, Vector3i blockPosition) {
    private BlockHit(BlockHitResult hitResult) {
      this(hitResult.getLocation(), hitResult.getBlockPos());
    }

    private BlockHit(Vec3 pos, BlockPos bp) {
      this(new Vector3d(pos.x, pos.y, pos.z), new Vector3i(bp.getX(), bp.getY(), bp.getZ()));
    }
  }
}
