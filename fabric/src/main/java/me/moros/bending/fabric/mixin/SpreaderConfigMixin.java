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

package me.moros.bending.fabric.mixin;

import me.moros.bending.fabric.event.ServerBlockEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.MultifaceSpreader.SpreadPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultifaceSpreader.DefaultSpreaderConfig.class)
public abstract class SpreaderConfigMixin {
  @Inject(method = "canSpreadInto", at = @At(value = "HEAD"), cancellable = true)
  private void bending$canSpreadInto(BlockGetter blockGetter, BlockPos blockPos, SpreadPos spreadPos, CallbackInfoReturnable<Boolean> cir) {
    if (blockGetter instanceof ServerLevel level && !ServerBlockEvents.SPREAD.invoker().onSpread(level, blockPos, spreadPos.pos())) {
      cir.setReturnValue(false);
    }
  }
}
