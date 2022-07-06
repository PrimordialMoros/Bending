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

package me.moros.bending.event;

import java.util.Collection;

import me.moros.bending.event.BindChangeEvent.BindType;
import me.moros.bending.event.ElementChangeEvent.ElementAction;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The event bus is responsible for posting bending events.
 */
public enum EventBus {
  INSTANCE;

  public void postPlayerLoadEvent(@NonNull BendingPlayer player) {
    new BendingPlayerLoadEvent(player).callEvent();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postCooldownAddEvent(@NonNull User user, @NonNull AbilityDescription desc, long duration) {
    return new CooldownAddEvent(user, desc, duration).callEvent();
  }

  public void postCooldownRemoveEvent(@NonNull User user, @NonNull AbilityDescription desc) {
    new CooldownRemoveEvent(user, desc).callEvent();
  }

  public void postAbilityActivationEvent(@NonNull User user, @NonNull AbilityDescription desc) {
    new AbilityActivationEvent(user, desc).callEvent();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postElementChangeEvent(@NonNull User user, @NonNull ElementAction type) {
    return new ElementChangeEvent(user, type).callEvent();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postBindChangeEvent(@NonNull User user, @NonNull BindType type) {
    return new BindChangeEvent(user, type).callEvent();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postPresetCreateEvent(@NonNull User user, @NonNull Preset preset) {
    return !preset.isEmpty() && new PresetCreateEvent(user, preset).callEvent();
  }

  public @NonNull BendingTickEffectEvent postTickEffectEvent(@NonNull User source, @NonNull Entity target, int duration, @NonNull BendingEffect type) {
    BendingTickEffectEvent event = new BendingTickEffectEvent(source, target, duration, type);
    event.callEvent();
    return event;
  }

  public @NonNull BendingDamageEvent postAbilityDamageEvent(@NonNull User source, @NonNull Entity target, @NonNull AbilityDescription desc, double damage) {
    BendingDamageEvent event = new BendingDamageEvent(source, target, desc, damage);
    event.callEvent();
    return event;
  }

  public @NonNull BendingExplosionEvent postExplosionEvent(@NonNull User source, @NonNull Vector3d center, @NonNull Collection<@NonNull Block> blocks, double power) {
    BendingExplosionEvent event = new BendingExplosionEvent(source, center, blocks, (float) power);
    event.callEvent();
    return event;
  }

  public @NonNull BendingLimitEvent postLimitEvent(@NonNull User source, @NonNull LivingEntity target, long duration) {
    BendingLimitEvent event = new BendingLimitEvent(source, target, duration);
    event.callEvent();
    return event;
  }

  public @NonNull BendingVelocityEvent postVelocityEvent(@NonNull User source, @NonNull LivingEntity target, @NonNull AbilityDescription desc, @NonNull Vector3d velocity) {
    BendingVelocityEvent event = new BendingVelocityEvent(source, target, desc, velocity);
    event.callEvent();
    return event;
  }
}
