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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin extends EntityMixin {
  @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
  public void bending$onHit(HitResult hitResult, CallbackInfo ci) {
    if (this.level().isClientSide() || hitResult.getType() == Type.MISS) {
      return;
    }
    if (!ServerEntityEvents.PROJECTILE_HIT.invoker().onProjectileHit((Projectile) (Object) this, hitResult)) {
      ci.cancel();
    }
  }
}
