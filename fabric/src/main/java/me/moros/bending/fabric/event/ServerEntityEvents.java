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

import me.moros.math.Vector3d;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
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

  // TODO mixin for entities and vehicle passengers?
  public static final Event<EntityMove> ENTITY_MOVE = EventFactory.createArrayBacked(EntityMove.class, callbacks -> (entity, from, to) -> {
    for (var callback : callbacks) {
      if (!callback.onMove(entity, from, to)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<Target> TARGET = EventFactory.createArrayBacked(Target.class, callbacks -> (entity, target) -> {
    for (var callback : callbacks) {
      if (!callback.onEntityTarget(entity, target)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<ProjectileHit> PROJECTILE_HIT = EventFactory.createArrayBacked(ProjectileHit.class, callbacks -> (projectile, hitResult) -> {
    for (var callback : callbacks) {
      if (!callback.onProjectileHit(projectile, hitResult)) {
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
  public interface EntityMove {
    boolean onMove(LivingEntity entity, Vector3d from, Vector3d to);
  }

  @FunctionalInterface
  public interface Target {
    boolean onEntityTarget(LivingEntity entity, Entity target);
  }

  @FunctionalInterface
  public interface ProjectileHit {
    boolean onProjectileHit(Projectile projectile, HitResult hitResult);
  }
}
