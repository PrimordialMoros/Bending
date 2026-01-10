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

package me.moros.bending.fabric.mixin.block.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.fabric.event.ServerItemEvents;
import net.minecraft.world.LockCode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin implements Lockable {
  @Shadow
  private LockCode lockKey;

  @Override
  public boolean hasLock() {
    return lockKey != LockCode.NO_LOCK;
  }

  @Override
  public void unlock() {
    lockKey = LockCode.NO_LOCK;
  }

  @ModifyExpressionValue(
    method = "createMenu",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/LockCode;canUnlock(Lnet/minecraft/world/entity/player/Player;)Z")
  )
  private boolean bending$canUnlock(boolean original, @Local(argsOnly = true) Player player) {
    if (!player.isSpectator() && ServerItemEvents.ACCESS_LOCK.invoker().onAccess(player, this)) {
      return true;
    }
    return original;
  }
}
