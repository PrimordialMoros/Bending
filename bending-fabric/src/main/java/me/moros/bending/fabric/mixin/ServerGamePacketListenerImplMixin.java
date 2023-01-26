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

import me.moros.bending.fabric.event.ServerPlayerEvents;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
  @Shadow
  public ServerPlayer player;

  @Inject(method = "handleAnimate", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), cancellable = true)
  public void bending$interactEvent(ServerboundSwingPacket packet, CallbackInfo ci) {
    if (ServerPlayerEvents.INTERACT.invoker().onInteract(this.player, packet.getHand()) != InteractionResult.PASS) {
      ci.cancel();
    }
  }

  @Inject(method = "handlePlayerCommand", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), cancellable = true)
  public void bending$handlePlayerCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
    var action = packet.getAction();
    switch (action) {
      case PRESS_SHIFT_KEY, RELEASE_SHIFT_KEY -> {
        if (!ServerPlayerEvents.TOGGLE_SNEAK.invoker().onSneak(this.player, action == Action.PRESS_SHIFT_KEY)) {
          ci.cancel();
        }
      }
      case START_SPRINTING, STOP_SPRINTING -> {
        if (!ServerPlayerEvents.TOGGLE_SPRINT.invoker().onSprint(this.player, action == Action.START_SPRINTING)) {
          ci.cancel();
        }
      }
    }
  }
}
