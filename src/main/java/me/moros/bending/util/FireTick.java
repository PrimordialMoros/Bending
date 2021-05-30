/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.Bending;
import me.moros.bending.events.BendingCombustEvent;
import me.moros.bending.model.user.User;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class FireTick {
  public static final int MAX_TICKS = 100;
  private static final Map<LivingEntity, User> INSTANCES = new ConcurrentHashMap<>();

  private FireTick() {
  }

  public static void cleanup() {
    INSTANCES.keySet().removeIf(e -> !e.isValid() || e.getFireTicks() <= 0);
  }

  public static void extinguish(@NonNull Entity entity) {
    entity.setFireTicks(-1);
    if (entity instanceof LivingEntity) {
      INSTANCES.remove((LivingEntity) entity);
    }
  }

  /**
   * Visual only
   */
  public static void ignite(@NonNull User source, @NonNull Entity entity) {
    ignite(source, entity, 15);
  }

  public static void ignite(@NonNull User source, @NonNull Entity entity, int ticks) {
    if (ticks <= 0) {
      return;
    }
    int duration = NumberConversions.ceil(Math.min(ticks, MAX_TICKS) / 20.0);
    BendingCombustEvent event = Bending.eventBus().postBendingCombustEvent(source, entity, duration);
    if (!event.isCancelled() && entity.getFireTicks() < ticks) {
      entity.setFireTicks(ticks);
      trackEntity(entity, source);
    }
  }

  private static void trackEntity(Entity entity, User source) {
    if (entity instanceof LivingEntity) {
      INSTANCES.put((LivingEntity) entity, source);
    }
  }
}

