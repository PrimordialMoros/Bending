/*
 * Copyright 2020-2025 Moros
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

import java.util.EnumSet;
import java.util.Set;

import me.moros.bending.fabric.event.ServerEntityEvents;
import me.moros.bending.fabric.event.ServerPlayerEvents;
import me.moros.math.Vector3d;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
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
  public abstract void teleport(PositionMoveRotation positionMoveRotation, Set<Relative> relativeArguments);

  @Inject(method = "handleAnimate", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), cancellable = true)
  private void bending$onInteractEvent(ServerboundSwingPacket packet, CallbackInfo ci) {
    if (ServerPlayerEvents.INTERACT.invoker().onInteract(this.player, packet.getHand()) != InteractionResult.PASS) {
      ci.cancel();
    }
  }

  @Inject(method = "handlePlayerCommand", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), cancellable = true)
  private void bending$onHandlePlayerCommand(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
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

  @Inject(method = "handleSetCarriedItem",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundSetCarriedItemPacket;getSlot()I"),
    slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;stopUsingItem()V"))
  )
  private void bending$onHandleSetCarriedItem(ServerboundSetCarriedItemPacket packet, CallbackInfo ci) {
    int oldSlot = this.player.getInventory().selected;
    int newSlot = packet.getSlot();
    ServerPlayerEvents.CHANGE_SLOT.invoker().onHeldSlotChange(this.player, oldSlot, newSlot);
  }

  @Inject(method = "handleMovePlayer",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isPassenger()Z"),
    cancellable = true
  )
  private void bending$onHandleMovePlayer(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
    boolean fireMoveEvent = packet.hasPosition();
    // During login, minecraft sends a packet containing neither the 'moving' or 'rotating' flag set - but only once.
    if (!fireMoveEvent && !packet.hasRotation()) {
      return;
    }
    var from = Vector3d.of(this.player.getX(), this.player.getY(), this.player.getZ());
    var to = Vector3d.of(packet.getX(from.x()), packet.getY(from.y()), packet.getZ(from.z()));
    float xRot = this.player.getXRot();
    float yRot = this.player.getYRot();
    if (fireMoveEvent && !ServerEntityEvents.ENTITY_MOVE.invoker().onMove(this.player, from, to)) {
      double x = from.x();
      double y = from.y();
      double z = from.z();
      this.player.absMoveTo(x, y, z, xRot, yRot);
      PositionMoveRotation positionMoveRotation = new PositionMoveRotation(new Vec3(x, y, z), Vec3.ZERO, xRot, yRot);
      this.teleport(positionMoveRotation, EnumSet.of(Relative.X_ROT, Relative.Y_ROT));
      ci.cancel();
    }
  }
}
