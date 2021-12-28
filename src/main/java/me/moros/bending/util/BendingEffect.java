/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.util;

import java.util.HashMap;
import java.util.Map;

import me.moros.bending.Bending;
import me.moros.bending.event.BendingTickEffectEvent;
import me.moros.bending.model.user.User;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum BendingEffect {
  FROST_TICK(135, true, Entity::getMaxFreezeTicks, Entity::getFreezeTicks, Entity::setFreezeTicks),
  FIRE_TICK(15, false, Entity::getMaxFireTicks, Entity::getFireTicks, Entity::setFireTicks);

  public static final int MAX_BLOCK_FIRE_TICKS = 100;

  private final Map<LivingEntity, User> instances;
  private final int visual;
  private final boolean cumulative;
  private final TickGetter maxTicks;
  private final TickGetter currentTicks;
  private final TickSetter handler;

  BendingEffect(int visual, boolean cumulative, TickGetter maxTicks, TickGetter getter, TickSetter setter) {
    instances = new HashMap<>();
    this.visual = visual;
    this.cumulative = cumulative;
    this.maxTicks = maxTicks;
    this.currentTicks = getter;
    this.handler = setter;
  }

  public void apply(@NonNull User source, @NonNull Entity entity) {
    apply(source, entity, visual);
  }

  public void apply(@NonNull User source, @NonNull Entity entity, int ticks) {
    if (ticks <= 0) {
      return;
    }
    BendingTickEffectEvent event = Bending.eventBus().postTickEffectEvent(source, entity, ticks, this);
    if (event.isCancelled()) {
      return;
    }
    if (currentTicks.get(entity) < ticks) {
      int current = Math.max(0, currentTicks.get(entity));
      handler.set(entity, Math.min(maxTicks.get(entity), cumulative ? current + ticks : ticks));
      trackEntity(entity, source);
    }
  }

  public void reset(@NonNull Entity entity) {
    handler.set(entity, -1);
    if (entity instanceof LivingEntity livingEntity) {
      instances.remove(livingEntity);
    }
  }

  public @Nullable User tickSource(@NonNull LivingEntity entity) {
    return instances.get(entity);
  }

  void trackEntity(Entity entity, User source) {
    if (entity instanceof LivingEntity livingEntity) {
      instances.put(livingEntity, source);
    }
  }

  public static void cleanup() {
    for (BendingEffect tick : values()) {
      tick.instances.keySet().removeIf(e -> !e.isValid() || e.getFireTicks() <= 0);
    }
  }

  public static void resetAll(@NonNull Entity entity) {
    for (BendingEffect tick : values()) {
      tick.reset(entity);
    }
  }

  @FunctionalInterface
  private interface TickGetter {
    int get(Entity entity);
  }

  @FunctionalInterface
  private interface TickSetter {
    void set(Entity entity, int ticks);
  }
}

