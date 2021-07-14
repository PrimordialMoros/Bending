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

package me.moros.bending.events;

import java.util.Collection;

import me.moros.bending.Bending;
import me.moros.bending.events.BindChangeEvent.BindType;
import me.moros.bending.events.ElementChangeEvent.ElementAction;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * The event bus is responsible for posting bending events.
 */
public final class BendingEventBus {
  private final PluginManager manager;

  public BendingEventBus(@NonNull Bending plugin) {
    manager = plugin.getServer().getPluginManager();
  }

  public void postPlayerLoadEvent(@NonNull BendingPlayer player) {
    manager.callEvent(new BendingPlayerLoadEvent(player));
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postCooldownAddEvent(@NonNull User user, @NonNull AbilityDescription desc, long duration) {
    CooldownAddEvent event = new CooldownAddEvent(user, desc, duration);
    manager.callEvent(event);
    return !event.isCancelled();
  }

  public void postCooldownRemoveEvent(@NonNull User user, @NonNull AbilityDescription desc) {
    manager.callEvent(new CooldownRemoveEvent(user, desc));
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postElementChangeEvent(@NonNull User user, @NonNull ElementAction type) {
    ElementChangeEvent event = new ElementChangeEvent(user, type);
    manager.callEvent(event);
    return !event.isCancelled();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postBindChangeEvent(@NonNull User user, @NonNull BindType type) {
    BindChangeEvent event = new BindChangeEvent(user, type);
    manager.callEvent(event);
    return !event.isCancelled();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postPresetCreateEvent(@NonNull User user, @NonNull Preset preset) {
    if (preset.isEmpty()) {
      return false;
    }
    PresetCreateEvent event = new PresetCreateEvent(user, preset);
    manager.callEvent(event);
    return !event.isCancelled();
  }

  public @NonNull BendingCombustEvent postCombustEvent(@NonNull User source, @NonNull Entity target, int duration) {
    BendingCombustEvent event = new BendingCombustEvent(source, target, duration);
    manager.callEvent(event);
    return event;
  }

  public @NonNull BendingDamageEvent postAbilityDamageEvent(@NonNull User source, @NonNull Entity target, @NonNull AbilityDescription desc, double damage) {
    BendingDamageEvent event = new BendingDamageEvent(source, target, desc, damage);
    manager.callEvent(event);
    return event;
  }

  public @NonNull BendingExplosionEvent postExplosionEvent(@NonNull User source, @NonNull Location center, @NonNull Collection<@NonNull Block> blocks, double power) {
    BendingExplosionEvent event = new BendingExplosionEvent(source, center.clone(), blocks, (float) power);
    manager.callEvent(event);
    return event;
  }

  public @NonNull BendingRestrictEvent postRestrictEvent(@NonNull User source, @NonNull LivingEntity target, long duration) {
    BendingRestrictEvent event = new BendingRestrictEvent(source, target, duration);
    manager.callEvent(event);
    return event;
  }

  public @NonNull BendingVelocityEvent postVelocityEvent(@NonNull User source, @NonNull LivingEntity target, @NonNull AbilityDescription desc, @NonNull Vector3d velocity) {
    BendingVelocityEvent event = new BendingVelocityEvent(source, target, desc, velocity);
    manager.callEvent(event);
    return event;
  }
}
