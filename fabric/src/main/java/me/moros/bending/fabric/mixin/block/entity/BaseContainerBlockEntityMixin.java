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

package me.moros.bending.fabric.mixin.block.entity;

import java.util.Optional;

import me.moros.bending.api.platform.block.Lockable;
import me.moros.bending.fabric.mixin.accessor.LockCodeAccess;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.world.LockCode;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityMixin implements Lockable {
  @Shadow
  private LockCode lockKey;

  @Override
  public Optional<String> lock() {
    var pass = ((LockCodeAccess) lockKey).password();
    return pass.isBlank() ? Optional.empty() : Optional.of(pass);
  }

  @Override
  public void lock(Component lock) {
    lock(LegacyComponentSerializer.legacySection().serializeOr(lock, ""));
  }

  @Override
  public void lock(String lock) {
    if (lock.isBlank()) {
      unlock();
    } else {
      lockKey = new LockCode(lock);
    }
  }

  @Override
  public void unlock() {
    lockKey = LockCode.NO_LOCK;
  }
}
