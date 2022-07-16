/*
 * Copyright 2020-2022 Moros
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.event.EventBus;
import me.moros.bending.event.TickEffectEvent;
import me.moros.bending.model.user.User;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents special effects applied by bending abilities.
 */
public enum BendingEffect {
  /**
   * FreezeTick effect that freezes the target as if they were in powdered snow.
   * This effect is applied cumulatively.
   */
  FROST_TICK(135, true, Entity::getMaxFreezeTicks, Entity::getFreezeTicks, Entity::setFreezeTicks),
  /**
   * FireTick effect that burns the target as if they were standing in fire.
   * Applying this effect overrides previous instances of it.
   */
  FIRE_TICK(15, false, e -> 100, Entity::getFireTicks, Entity::setFireTicks);

  public static final int MAX_BLOCK_FIRE_TICKS = 100;

  private final Map<LivingEntity, User> instances;
  private final int visual;
  private final boolean cumulative;
  private final TickGetter maxTicks;
  private final TickGetter currentTicks;
  private final TickSetter handler;

  BendingEffect(int visual, boolean cumulative, TickGetter maxTicks, TickGetter getter, TickSetter setter) {
    instances = new ConcurrentHashMap<>();
    this.visual = visual;
    this.cumulative = cumulative;
    this.maxTicks = maxTicks;
    this.currentTicks = getter;
    this.handler = setter;
  }

  /**
   * Apply this effect.
   * @param source the user causing the effect
   * @param entity the target entity to apply the effect to
   */
  public void apply(User source, Entity entity) {
    apply(source, entity, visual);
  }

  /**
   * Apply this effect.
   * @param source the user causing the effect
   * @param entity the target entity to apply the effect to
   * @param ticks the duration of the effect in ticks
   */
  public void apply(User source, Entity entity, int ticks) {
    if (ticks <= 0) {
      return;
    }
    TickEffectEvent event = EventBus.INSTANCE.postTickEffectEvent(source, entity, ticks, this);
    if (event.isCancelled()) {
      return;
    }
    int current = Math.max(0, currentTicks.get(entity));
    if (current < ticks) {
      handler.set(entity, Math.min(maxTicks.get(entity), cumulative ? current + ticks : ticks));
      trackEntity(entity, source);
    }
  }

  /**
   * Reset and remove this effect from the specified entity.
   * @param entity the entity to remove the effect from
   */
  public void reset(Entity entity) {
    handler.set(entity, -1);
    if (entity instanceof LivingEntity livingEntity) {
      instances.remove(livingEntity);
    }
  }

  /**
   * Get the last user that applied this effect to the specified target.
   * @param entity the entity to check
   * @return the user that applied this effect if found, null otherwise
   */
  public @Nullable User tickSource(LivingEntity entity) {
    return instances.get(entity);
  }

  private void trackEntity(Entity entity, User source) {
    if (entity instanceof LivingEntity livingEntity) {
      instances.put(livingEntity, source);
    }
  }

  public static void cleanup() {
    for (BendingEffect tick : values()) {
      tick.instances.keySet().removeIf(e -> !e.isValid() || tick.currentTicks.get(e) <= 0);
    }
  }

  public static void resetAll(Entity entity) {
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

