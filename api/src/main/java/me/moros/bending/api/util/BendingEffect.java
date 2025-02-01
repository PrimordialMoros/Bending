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

package me.moros.bending.api.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.api.event.TickEffectEvent;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.entity.EntityUtil;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.property.IntegerProperty;
import me.moros.bending.api.user.User;
import me.moros.math.FastMath;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents special effects applied by bending abilities.
 */
public enum BendingEffect {
  /**
   * FreezeTick effect that freezes the target as if they were in powdered snow.
   * This effect is applied cumulatively.
   */
  FROST_TICK(135, true, 140, EntityProperties.FREEZE_TICKS),
  /**
   * FireTick effect that burns the target as if they were standing in fire.
   * Applying this effect overrides previous instances of it.
   */
  FIRE_TICK(15, false, 100, EntityProperties.FIRE_TICKS);

  public static final int MAX_BLOCK_FIRE_TICKS = 100;

  private final Map<LivingEntity, User> instances;
  private final int visual;
  private final boolean cumulative;
  private final int maxTicks;
  private final IntegerProperty property;

  BendingEffect(int visual, boolean cumulative, int maxTicks, IntegerProperty property) {
    instances = new ConcurrentHashMap<>();
    this.visual = visual;
    this.cumulative = cumulative;
    this.maxTicks = maxTicks;
    this.property = property;
  }

  private int getCurrentTicks(Entity entity) {
    return entity.propertyValue(property);
  }

  private void setCurrentTicks(Entity entity, int value) {
    entity.setProperty(property, value);
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
    TickEffectEvent event = source.game().eventBus().postTickEffectEvent(source, entity, ticks, this);
    int duration = event.duration();
    if (event.cancelled() || duration <= 0) {
      return;
    }
    if (this == FROST_TICK) {
      handleFreeze(entity, duration);
    }
    int current = Math.max(0, getCurrentTicks(entity));
    if (current < duration) {
      setCurrentTicks(entity, Math.min(maxTicks, cumulative ? current + duration : duration));
      trackEntity(entity, source);
    }
  }

  private void handleFreeze(Entity entity, int duration) {
    if (duration >= 30 && entity instanceof LivingEntity living) {
      int potionDuration = FastMath.round(0.5 * duration);
      int power = FastMath.floor(duration / 30.0);
      EntityUtil.tryAddPotion(living, PotionEffect.SLOWNESS, potionDuration, power);
    }
  }

  /**
   * Reset and remove this effect from the specified entity.
   * @param entity the entity to remove the effect from
   */
  public void reset(Entity entity) {
    setCurrentTicks(entity, -1);
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
      tick.instances.keySet().removeIf(e -> !e.valid() || tick.getCurrentTicks(e) <= 0);
    }
  }

  public static void resetAll(Entity entity) {
    for (BendingEffect tick : values()) {
      tick.reset(entity);
    }
  }
}

