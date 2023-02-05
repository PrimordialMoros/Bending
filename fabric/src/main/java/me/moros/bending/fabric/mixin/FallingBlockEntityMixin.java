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

package me.moros.bending.fabric.mixin;

import me.moros.bending.fabric.event.ServerEntityEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends EntityMixin {
  @Inject(method = "fall",
    at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"),
    cancellable = true,
    locals = LocalCapture.CAPTURE_FAILHARD
  )
  private static void bending$onFall(Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<FallingBlockEntity> cir, FallingBlockEntity entity) {
    if (level.isClientSide) {
      return;
    }
    if (!ServerEntityEvents.FALLING_BLOCK.invoker().onFall((ServerLevel) level, pos)) {
      cir.setReturnValue(entity);
    }
  }

  @Inject(method = "tick",
    at = @At(value = "INVOKE",
      target = "Lnet/minecraft/world/level/Level;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z",
      shift = Shift.AFTER
    ),
    cancellable = true
  )
  private void bending$onFall(HitResult hitResult, CallbackInfoReturnable<ItemEntity> ci) {
    if (this.level.isClientSide) {
      return;
    }
    if (!ServerEntityEvents.PROJECTILE_HIT.invoker().onHit((Projectile) (Object) this, hitResult)) {
      ci.cancel();
    }
  }
}
