/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.fabric.mixin.block;

import java.util.List;

import com.llamalad7.mixinextras.sugar.Local;
import me.moros.bending.fabric.event.ServerBlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBaseBlockMixin {
  @Inject(method = "moveBlocks", at = @At(value = "INVOKE_ASSIGN",
    target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToDestroy()Ljava/util/List;"),
    cancellable = true
  )
  private void bending$onMoveBlocks(Level level, BlockPos blockPos, Direction direction, boolean extending,
                                    CallbackInfoReturnable<Boolean> cir, @Local PistonStructureResolver helper) {
    if (level.isClientSide()) {
      return;
    }
    List<BlockPos> toPush = helper.getToPush();
    List<BlockPos> toDestroy = helper.getToDestroy();
    if (!ServerBlockEvents.PISTON.invoker().onPistonMove((ServerLevel) level, blockPos, toPush, toDestroy)) {
      for (BlockPos b : toDestroy) {
        level.sendBlockUpdated(b, Blocks.AIR.defaultBlockState(), level.getBlockState(b), 3);
      }
      for (BlockPos b : toPush) {
        level.sendBlockUpdated(b, Blocks.AIR.defaultBlockState(), level.getBlockState(b), 3);
        b = b.relative(direction);
        level.sendBlockUpdated(b, Blocks.AIR.defaultBlockState(), level.getBlockState(b), 3);
      }
      cir.setReturnValue(false);
    }
  }
}
