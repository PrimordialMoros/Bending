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

import me.moros.bending.fabric.event.ServerPlayerEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {
  @Shadow
  public abstract ItemStack getItem();

  @Shadow
  public abstract void set(ItemStack itemStack);

  @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
  private void bending$mayPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
    var itemStack = getItem();
    if (!itemStack.isEmpty() && player instanceof ServerPlayer serverPlayer) {
      if (!ServerPlayerEvents.MODIFY_INVENTORY_SLOT.invoker().onModify(serverPlayer, itemStack)) {
        if (itemStack.getCount() <= 0) {
          set(ItemStack.EMPTY);
        }
        cir.setReturnValue(false);
      }
    }
  }
}
