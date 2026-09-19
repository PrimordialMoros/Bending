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

import java.util.Set;

import me.moros.bending.fabric.event.ServerEntityEvents;
import me.moros.bending.fabric.event.ServerPlayerEvents;
import me.moros.math.Vector3d;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
  @Shadow
  public ServerPlayer player;

  @Shadow
  public abstract void teleport(PositionMoveRotation destination, Set<Relative> relatives);

  @Shadow public abstract boolean hasClientLoaded();

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

  @Inject(method = "isEntityCollidingWithAnythingNew", at = @At(value = "HEAD"), cancellable = true)
  private void bending$isEntityCollidingWithAnythingNew(LevelReader level, Entity entity, AABB oldAABB,
                                                        double newX, double newY, double newZ,
                                                        CallbackInfoReturnable<Boolean> cir) {
    if (level instanceof ServerLevel && entity instanceof LivingEntity livingEntity) {
      var from = Vector3d.of(livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
      var to = Vector3d.of(newX, newY, newZ);
      if (!ServerEntityEvents.ENTITY_MOVE.invoker().onMove(livingEntity, from, to)) {
        cir.setReturnValue(true);
      }
    }
  }
}
