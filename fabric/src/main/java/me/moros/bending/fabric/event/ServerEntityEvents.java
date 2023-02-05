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

package me.moros.bending.fabric.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

public final class ServerEntityEvents {
  private ServerEntityEvents() {
  }

  public static final Event<Merge> MERGE = EventFactory.createArrayBacked(Merge.class, callbacks -> (first, second) -> {
    for (var callback : callbacks) {
      if (!callback.onMerge(first, second)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<Damage> DAMAGE = EventFactory.createArrayBacked(Damage.class, callbacks -> (entity, source, damage) -> {
    double value = damage;
    for (var callback : callbacks) {
      value = callback.onDamage(entity, source, value);
      if (value <= 0) {
        return 0;
      }
    }
    return value;
  });

  public static final Event<DropItem> DROP_ITEM = EventFactory.createArrayBacked(DropItem.class, callbacks -> (level, stack) -> {
    for (var callback : callbacks) {
      if (!callback.onDropItem(level, stack)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<ProjectileHit> PROJECTILE_HIT = EventFactory.createArrayBacked(ProjectileHit.class, callbacks -> (projectile, hitResult) -> {
    for (var callback : callbacks) {
      if (!callback.onHit(projectile, hitResult)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<FallingBlock> FALLING_BLOCK = EventFactory.createArrayBacked(FallingBlock.class, callbacks -> (level, blockPos) -> {
    for (var callback : callbacks) {
      if (!callback.onFall(level, blockPos)) {
        return false;
      }
    }
    return true;
  });


  @FunctionalInterface
  public interface Merge {
    boolean onMerge(ItemEntity first, ItemEntity second);
  }

  @FunctionalInterface
  public interface Damage {
    double onDamage(LivingEntity entity, DamageSource source, double damage);
  }

  @FunctionalInterface
  public interface DropItem {
    boolean onDropItem(ServerLevel level, ItemStack stack);
  }

  @FunctionalInterface
  public interface ProjectileHit {
    boolean onHit(Projectile projectile, HitResult result);
  }

  @FunctionalInterface
  public interface FallingBlock {
    boolean onFall(ServerLevel level, BlockPos pos);
  }
}
