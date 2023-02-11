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

package me.moros.bending.fabric.mixin.block;

import java.util.List;
import java.util.function.Consumer;

import me.moros.bending.fabric.event.ServerItemEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Block.class)
public abstract class BlockMixin {
  @Redirect(
    method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
    at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V")
  )
  private static void bending$dropResources(List<ItemStack> stacks, Consumer<ItemStack> action, BlockState state, Level level, BlockPos blockPos, @Nullable BlockEntity blockEntity, Entity entity, ItemStack stack) {
    bending$filterDrops(stacks, action, (ServerLevel) level, blockPos);
  }


  @Redirect(
    method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
    at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V")
  )
  private static void bending$dropResources(List<ItemStack> stacks, Consumer<ItemStack> action, BlockState state, LevelAccessor levelAccessor, BlockPos blockPos) {
    bending$filterDrops(stacks, action, (ServerLevel) levelAccessor, blockPos);
  }

  @Redirect(
    method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V",
    at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V")
  )
  private static void bending$dropResources(List<ItemStack> stacks, Consumer<ItemStack> action, BlockState state, Level level, BlockPos blockPos) {
    bending$filterDrops(stacks, action, (ServerLevel) level, blockPos);
  }

  @Unique
  private static void bending$filterDrops(List<ItemStack> stacks, Consumer<ItemStack> action, ServerLevel level, BlockPos pos) {
    var result = ServerItemEvents.BLOCK_DROP_LOOT.invoker().onDropLoot(level, pos, stacks);
    if (result.getResult() != InteractionResult.FAIL) {
      result.getObject().forEach(action);
    }
  }
}
