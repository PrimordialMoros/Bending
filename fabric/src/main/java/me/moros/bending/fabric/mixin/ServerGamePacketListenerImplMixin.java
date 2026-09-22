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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import me.moros.bending.fabric.event.ServerEntityEvents;
import me.moros.bending.fabric.event.ServerPlayerEvents;
import me.moros.math.Vector3d;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
  @Shadow
  public ServerPlayer player;

  @Shadow
  public abstract void teleport(double x, double y, double z, float yRot, float xRot);

  @Shadow
  public abstract boolean hasClientLoaded();

  @Inject(method = "handlePunch", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), cancellable = true)
  private void bending$onInteractEvent(ServerboundPunchPacket packet, CallbackInfo ci) {
    if (ServerPlayerEvents.INTERACT.invoker().onPunch(this.player) != InteractionResult.PASS) {
      ci.cancel();
    }
  }

  @Inject(method = "handlePlayerInput", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/server/level/ServerPlayer;setLastClientInput(Lnet/minecraft/world/entity/player/Input;)V"), cancellable = true)
  private void bending$onHandlePlayerInput(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
    var lastInput = this.player.getLastClientInput();
    boolean shiftKeyDown = packet.input().shift();
    if (lastInput.shift() != shiftKeyDown) {
      if (!ServerPlayerEvents.TOGGLE_SNEAK.invoker().onSneak(this.player, shiftKeyDown)) {
        ci.cancel();
        shiftKeyDown = this.player.isShiftKeyDown();
        if (hasClientLoaded()) {
          this.player.resetLastActionTime();
          this.player.setShiftKeyDown(shiftKeyDown);
        }
      }
    }
  }

  @Inject(method = "handlePlayerCommand", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), cancellable = true)
  private void bending$onHandlePlayerCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
    var action = packet.getAction();
    switch (action) {
      case START_SPRINTING, STOP_SPRINTING -> {
        if (!ServerPlayerEvents.TOGGLE_SPRINT.invoker().onSprint(this.player, action == Action.START_SPRINTING)) {
          ci.cancel();
        }
      }
    }
  }

  @Inject(method = "handleSetCarriedItem",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundSetCarriedItemPacket;getSlot()I"),
    slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;stopUsingItem()V"))
  )
  private void bending$onHandleSetCarriedItem(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
    int oldSlot = this.player.getInventory().getSelectedSlot();
    int newSlot = packet.getSlot();
    ServerPlayerEvents.CHANGE_SLOT.invoker().onHeldSlotChange(this.player, oldSlot, newSlot);
  }

  @WrapOperation(
    method = "handlePlayerPositionChange",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;absSnapTo(DDDFF)V", ordinal = 1)
  )
  private void bending$onAbsSnapTo(ServerPlayer player, double targetX, double targetY, double targetZ, float targetYRot, float targetXRot, Operation<Void> original,
                                   @Local(argsOnly = true, name = "isOnGround") boolean isOnGround,
                                   @Local(name = "startX") double startX, @Local(name = "startY") double startY, @Local(name = "startZ") double startZ,
                                   @Cancellable CallbackInfo ci) {
    if (!ci.isCancelled()) {
      var from = Vector3d.of(startX, startY, startZ);
      var to = Vector3d.of(targetX, targetY, targetZ);
      if (!ServerEntityEvents.ENTITY_MOVE.invoker().onMove(player, from, to)) {
        this.teleport(startX, startY, startZ, targetYRot, targetXRot);
        this.player.doCheckFallDamage(this.player.getX() - startX, this.player.getY() - startY, this.player.getZ() - startZ, isOnGround);
        this.player.removeLatestMovementRecording();
        ci.cancel();
        return;
      }
    }
    original.call(player, targetX, targetY, targetZ, targetYRot, targetXRot);
  }
}
