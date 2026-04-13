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

package me.moros.bending.fabric.mixin.entity;

import java.util.function.Consumer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.moros.bending.fabric.event.ServerEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
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

  @Inject(method = "createItemStackToDrop",
    at = @At(value = "HEAD"), cancellable = true
  )
  private void bending$onDrop(ItemStack itemStack, boolean randomly, boolean thrownFromHand, CallbackInfoReturnable<ItemEntity> cir) {
    if (itemStack.isEmpty() || this.level().isClientSide()) {
      return;
    }
    if (!ServerEntityEvents.DROP_ITEM.invoker().onDrop((LivingEntity) (Object) this, itemStack)) {
      cir.setReturnValue(null);
    }
  }

  @WrapOperation(
    method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;ZLnet/minecraft/resources/ResourceKey;Ljava/util/function/Consumer;)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;JLjava/util/function/Consumer;)V")
  )
  private void bending$onDropLoot(LootTable instance, LootParams params, long optionalLootTableSeed, Consumer<ItemStack> output, Operation<Void> original) {
    if (this.level().isClientSide()) {
      original.call(instance, params, optionalLootTableSeed, output);
      return;
    }
    var loot = instance.getRandomItems(params, optionalLootTableSeed);
    var result = ServerEntityEvents.DROP_LOOT.invoker().onDropLoot((LivingEntity) (Object) this, loot);
    result.forEach(output);
  }
}
