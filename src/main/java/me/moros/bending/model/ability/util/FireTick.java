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

package me.moros.bending.model.ability.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.model.user.User;
import me.moros.bending.util.Tasker;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

public enum FireTick implements FireTickMethod {
  OVERWRITE(FireTick::igniteEntity),
  LARGER((u, e, t) -> {
    if (e.getFireTicks() < t) {
      igniteEntity(u, e, t);
    }
  }),
  ACCUMULATE((u, e, t) -> igniteEntity(u, e, FastMath.max(0, e.getFireTicks()) + t));

  private final FireTickMethod method;

  FireTick(FireTickMethod method) {
    this.method = method;
  }

  @Override
  public void apply(@NonNull User source, @NonNull Entity target, int ticks) {
    method.apply(source, target, ticks);
  }

  public static final int MAX_TICKS = 90;
  private static final Map<LivingEntity, User> INSTANCES = new ConcurrentHashMap<>();

  static {
    Tasker.repeatingTask(FireTick::cleanup, 5);
  }

  private static void cleanup() {
    INSTANCES.keySet().removeIf(e -> !e.isValid() || e.getFireTicks() <= 0);
  }

  public static void extinguish(@NonNull Entity entity) {
    entity.setFireTicks(-1);
    if (entity instanceof LivingEntity) {
      INSTANCES.remove((LivingEntity) entity);
    }
  }

  private static void igniteEntity(@NonNull User source, @NonNull Entity entity, int ticks) {
    if (ticks <= 0) {
      return;
    }
    if (ticks > MAX_TICKS) {
      ticks = MAX_TICKS;
    }
    if (entity instanceof LivingEntity) {
      int duration = NumberConversions.ceil(ticks / 20.0);
      EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(source.entity(), entity, duration);
      Bukkit.getPluginManager().callEvent(event);
      if (!event.isCancelled() && event.getDuration() > 0) {
        entity.setFireTicks(FastMath.min(ticks, event.getDuration() * 20));
        INSTANCES.put((LivingEntity) entity, source);
      }
    } else {
      entity.setFireTicks(ticks);
    }
  }
}

