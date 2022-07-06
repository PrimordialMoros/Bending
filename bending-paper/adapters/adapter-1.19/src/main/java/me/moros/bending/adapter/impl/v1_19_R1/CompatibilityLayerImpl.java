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

package me.moros.bending.adapter.impl.v1_19_R1;

import java.util.Set;
import java.util.stream.Collectors;

import me.moros.bending.adapter.CompatibilityLayer;
import me.moros.bending.adapter.PacketAdapter;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.model.raytrace.CompositeRayTrace;
import me.moros.bending.model.raytrace.RayTraceContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_19_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R1.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CompatibilityLayerImpl implements CompatibilityLayer {
  private final PacketAdapter packetAdapter;

  public CompatibilityLayerImpl() {
    packetAdapter = new PacketAdapterImpl(this);
  }

  @Override
  public boolean isNative() {
    return true;
  }

  @Override
  public @NonNull PacketAdapter packetAdapter() {
    return packetAdapter;
  }

  @NonNull ServerLevel world(@NonNull World world) {
    return ((CraftWorld) world).getHandle();
  }

  @NonNull BlockState blockState(@NonNull Block block) {
    return ((CraftBlock) block).getNMS();
  }

  @NonNull BlockState blockState(@NonNull BlockData data) {
    return ((CraftBlockData) data).getState();
  }

  @NonNull ServerPlayer player(@NonNull Player player) {
    return ((CraftPlayer) player).getHandle();
  }

  net.minecraft.world.entity.@NonNull Entity entity(@NonNull Entity entity) {
    return ((CraftEntity) entity).getHandle();
  }

  @NonNull ItemStack itemStack(@NonNull Material material) {
    return CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(material));
  }

  @Override
  public boolean setBlockFast(@NonNull Block block, @NonNull BlockData data) {
    BlockPos position = new BlockPos(block.getX(), block.getY(), block.getZ());
    return world(block.getWorld()).setBlock(position, blockState(data), 2);
  }

  @Override
  public boolean eyeInWater(@NonNull Entity entity) {
    return entity(entity).isEyeInFluid(FluidTags.WATER);
  }

  @Override
  public boolean eyeInLava(@NonNull Entity entity) {
    return entity(entity).isEyeInFluid(FluidTags.LAVA);
  }

  @Override
  public @NonNull CompositeRayTrace rayTraceBlocks(@NonNull RayTraceContext context, @NonNull World world) {
    Vector3d s = context.start();
    Vector3d e = context.end();
    Vec3 startPos = new Vec3(s.x(), s.y(), s.z());
    Vec3 endPos = new Vec3(e.x(), e.y(), e.z());

    ClipContext.Block ccb = context.ignorePassable() ? ClipContext.Block.COLLIDER : ClipContext.Block.OUTLINE;
    Fluid ccf = context.ignoreLiquids() ? Fluid.NONE : Fluid.ANY;
    ClipContext clipContext = new ClipContext(startPos, endPos, ccb, ccf, null);

    BlockHit miss = new BlockHit(clipContext.getTo(), new BlockPos(clipContext.getTo()), null);
    Set<BlockPos> ignored = context.ignoreBlocks().stream()
      .map(b -> new BlockPos(b.getX(), b.getY(), b.getZ())).collect(Collectors.toSet());

    BlockHit result = traverseBlocks(world, clipContext, ignored, miss);
    if (!miss.equals(result)) {
      Vector3i bp = result.blockPosition();
      return CompositeRayTrace.hit(result.position, world.getBlockAt(bp.x(), bp.y(), bp.z()), dirToFace(result.direction()));
    }
    return CompositeRayTrace.miss(result.position);
  }

  private BlockFace dirToFace(Direction direction) {
    if (direction == null) {
      return BlockFace.SELF;
    }
    return switch (direction) {
      case DOWN -> BlockFace.DOWN;
      case UP -> BlockFace.UP;
      case NORTH -> BlockFace.NORTH;
      case SOUTH -> BlockFace.SOUTH;
      case WEST -> BlockFace.WEST;
      case EAST -> BlockFace.EAST;
    };
  }

  private BlockHit traverseBlocks(World world, ClipContext context, Set<BlockPos> ignored, BlockHit miss) {
    Vec3 start = context.getFrom();
    Vec3 end = context.getTo();
    Level level = world(world);
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

  private record BlockHit(Vector3d position, Vector3i blockPosition, Direction direction) {
    private BlockHit(BlockHitResult hitResult) {
      this(hitResult.getLocation(), hitResult.getBlockPos(), hitResult.getDirection());
    }

    private BlockHit(Vec3 pos, BlockPos bp, Direction direction) {
      this(new Vector3d(pos.x, pos.y, pos.z), new Vector3i(bp.getX(), bp.getY(), bp.getZ()), direction);
    }
  }
}
