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

package me.moros.bending.fabric.mixin.entity;

import me.moros.bending.fabric.event.ServerEntityEvents;
import me.moros.bending.fabric.event.ServerItemEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 900)
public abstract class LivingEntityMixin extends EntityMixin {
  @ModifyVariable(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"), ordinal = 0, argsOnly = true)
  private float bending$onHurt(float originalValue, ServerLevel serverLevel, DamageSource source, float amount) {
    return (float) ServerEntityEvents.DAMAGE.invoker().onDamage((LivingEntity) (Object) this, source, originalValue);
  }

  @Inject(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
    at = @At(value = "RETURN", ordinal = 1), cancellable = true
  )
  private void bending$onDrop(ItemStack stack, boolean dropAround, boolean traceItem, CallbackInfoReturnable<ItemEntity> cir) {
    if (stack.isEmpty() || this.level().isClientSide) {
      return;
    }
    if (!ServerItemEvents.DROP_ITEM.invoker().onDrop((LivingEntity) (Object) this, stack)) {
      cir.setReturnValue(null);
    }
  }
}
