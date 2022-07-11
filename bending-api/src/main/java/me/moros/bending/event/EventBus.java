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
import me.moros.bending.model.key.Keyed;
import me.moros.bending.model.key.RegistryKey;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/**
 * The event bus is responsible for posting bending events.
 */
public enum EventBus {
  INSTANCE;

  public void postRegistryLockEvent(Collection<RegistryKey<? extends Keyed>> keys) {
    new RegistryLockEvent(keys).callEvent();
  }

  public void postPlayerRegisterEvent(BendingPlayer player) {
    new PlayerRegisterEvent(player).callEvent();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postCooldownAddEvent(User user, AbilityDescription desc, long duration) {
    return new CooldownAddEvent(user, desc, duration).callEvent();
  }

  public void postCooldownRemoveEvent(User user, AbilityDescription desc) {
    new CooldownRemoveEvent(user, desc).callEvent();
  }

  public void postAbilityActivationEvent(User user, AbilityDescription desc) {
    new AbilityActivationEvent(user, desc).callEvent();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postElementChangeEvent(User user, ElementAction type) {
    return new ElementChangeEvent(user, type).callEvent();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postBindChangeEvent(User user, BindType type) {
    return new BindChangeEvent(user, type).callEvent();
  }

  /**
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postPresetCreateEvent(User user, Preset preset) {
    return !preset.isEmpty() && new PresetCreateEvent(user, preset).callEvent();
  }

  public TickEffectEvent postTickEffectEvent(User source, Entity target, int duration, BendingEffect type) {
    TickEffectEvent event = new TickEffectEvent(source, target, duration, type);
    event.callEvent();
    return event;
  }

  public BendingDamageEvent postAbilityDamageEvent(User source, Entity target, AbilityDescription desc, double damage) {
    BendingDamageEvent event = new BendingDamageEvent(source, target, desc, damage);
    event.callEvent();
    return event;
  }

  public BendingExplosionEvent postExplosionEvent(User source, Vector3d center, Collection<Block> blocks, double power) {
    BendingExplosionEvent event = new BendingExplosionEvent(source, center, blocks, (float) power);
    event.callEvent();
    return event;
  }

  public ActionLimitEvent postActionLimitEvent(User source, LivingEntity target, long duration) {
    ActionLimitEvent event = new ActionLimitEvent(source, target, duration);
    event.callEvent();
    return event;
  }

  public VelocityEvent postVelocityEvent(User source, LivingEntity target, AbilityDescription desc, Vector3d velocity) {
    VelocityEvent event = new VelocityEvent(source, target, desc, velocity);
    event.callEvent();
    return event;
  }
}
