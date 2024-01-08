/*
 * Copyright 2020-2024 Moros
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
import me.moros.bending.fabric.event.ServerPlayerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameType;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
  @Shadow
  protected ServerLevel level;

  @Shadow
  @Final
  protected ServerPlayer player;

  @Inject(method = "setGameModeForPlayer", at = @At(value = "HEAD"))
  private void bending$onSetGameModeForPlayer(GameType gameType, @Nullable GameType gameType2, CallbackInfo ci) {
    ServerPlayerEvents.CHANGE_GAMEMODE.invoker().onGameModeChange(this.player, gameType);
  }

  @Inject(method = "destroyBlock", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/server/level/ServerPlayerGameMode;isCreative()Z"), cancellable = true
  )
  private void bending$onDestroyBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
    if (!ServerBlockEvents.AFTER_BREAK.invoker().onBreak(this.level, pos)) {
      cir.setReturnValue(true);
    }
  }
}
