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

package me.moros.bending.common.adapter;

import me.moros.bending.api.adapter.NativeAdapter;
import me.moros.bending.api.collision.raytrace.BlockRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractNativeAdapter extends AbstractPacketUtil implements NativeAdapter {
  protected AbstractNativeAdapter(PlayerList playerList) {
    super(playerList);
  }

  @Override
  public boolean setBlockFast(Block block, me.moros.bending.api.platform.block.BlockState state) {
    BlockPos position = new BlockPos(block.blockX(), block.blockY(), block.blockZ());
    return adapt(block.world()).setBlock(position, adapt(state), 2);
  }

  @Override
  public BlockRayTrace rayTraceBlocks(World world, Context context) {
    return RayTraceUtil.rayTraceBlocks(context, adapt(world), world);
  }

  @Override
  public boolean eyeInWater(Entity entity) {
    return adapt(entity).isEyeInFluid(FluidTags.WATER);
  }

  @Override
  public boolean eyeInLava(Entity entity) {
    return adapt(entity).isEyeInFluid(FluidTags.LAVA);
  }

  @Override
  public boolean tryPowerLightningRod(Block block) {
    ServerLevel level = adapt(block.world());
    BlockState data = level.getBlockState(new BlockPos(block.blockX(), block.blockY(), block.blockZ()));
    if (data.is(Blocks.LIGHTNING_ROD)) {
      BlockPos pos = new BlockPos(block.blockX(), block.blockY(), block.blockZ());
      ((LightningRodBlock) data.getBlock()).onLightningStrike(data, adapt(block.world()), pos);
      return true;
    }
    return false;
  }
}
