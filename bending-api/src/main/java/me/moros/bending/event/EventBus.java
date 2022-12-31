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

package me.moros.bending.event;

import java.util.Collection;
import java.util.function.Consumer;

import me.moros.bending.event.BindChangeEvent.BindType;
import me.moros.bending.event.ElementChangeEvent.ElementAction;
import me.moros.bending.event.base.BendingEvent;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.bending.util.BendingEffect;
import me.moros.math.Vector3d;
import net.kyori.adventure.key.Key;
import net.kyori.event.PostResult;

/**
 * The event bus is responsible for posting bending events.
 */
public enum EventBus {
  INSTANCE;

  private final net.kyori.event.EventBus<BendingEvent> eventBus;
  private boolean closed = false;

  EventBus() {
    this.eventBus = net.kyori.event.EventBus.create(BendingEvent.class);
  }

  /**
   * Close this event bus and stop sendi
   */
  public void shutdown() {
    this.eventBus.unsubscribeIf(x -> true);
    this.closed = true;
  }

  /**
   * Registers the given subscriber to receive events.
   * @param event the event type
   * @param subscriber the subscriber
   * @param <T> the event type
   */
  public <T extends BendingEvent> void subscribe(Class<T> event, Consumer<? super T> subscriber) {
    if (!closed) {
      eventBus.subscribe(event, subscriber::accept);
    }
  }

  private <T extends BendingEvent> PostResult post(T event) {
    if (closed) {
      throw new IllegalStateException("Eventbus has been terminated, cannot post new events!");
    }
    return eventBus.post(event);
  }

  /**
   * Posts a new {@link RegistryLockEvent}.
   * @param keys the RegistryKeys of all Registries that are going to be locked
   */
  public void postRegistryLockEvent(Collection<Key> keys) {
    post(new RegistryLockEvent(keys));
  }

  /**
   * Posts a new {@link PlayerRegisterEvent}.
   * @param player the player that was registered
   */
  public void postPlayerRegisterEvent(BendingPlayer player) {
    post(new PlayerRegisterEvent(player));
  }

  /**
   * Posts a new {@link CooldownAddEvent}.
   * @param user the relevant user
   * @param desc the ability to go on cooldown
   * @param duration the duration of the cooldown in milliseconds
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postCooldownAddEvent(User user, AbilityDescription desc, long duration) {
    return post(new CooldownAddEvent(user, desc, duration)).wasSuccessful();
  }

  /**
   * Posts a new {@link CooldownRemoveEvent}.
   * @param user the relevant user
   * @param desc the ability whose cooldown has expired
   */
  public void postCooldownRemoveEvent(User user, AbilityDescription desc) {
    post(new CooldownRemoveEvent(user, desc));
  }

  /**
   * Posts a new {@link AbilityActivationEvent}.
   * @param user the user who attempts to activate the ability
   * @param desc the ability that is being activated
   */
  public void postAbilityActivationEvent(User user, AbilityDescription desc) {
    post(new AbilityActivationEvent(user, desc));
  }

  /**
   * Posts a new {@link ElementChangeEvent}.
   * @param user the user who is changing elements
   * @param type the type of element change
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postElementChangeEvent(User user, ElementAction type) {
    return post(new ElementChangeEvent(user, type)).wasSuccessful();
  }

  /**
   * Posts a new {@link BindChangeEvent}.
   * @param user the user who is changing binds
   * @param type the type of bind change
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postBindChangeEvent(User user, BindType type) {
    return post(new BindChangeEvent(user, type)).wasSuccessful();
  }

  /**
   * Posts a new {@link PresetCreateEvent}.
   * @param user the user who is creating a new preset
   * @param preset the preset that is being created
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  public boolean postPresetCreateEvent(User user, Preset preset) {
    return !preset.isEmpty() && post(new PresetCreateEvent(user, preset)).wasSuccessful();
  }

  /**
   * Posts a new {@link TickEffectEvent}.
   * @param source the user responsible for applying the effect
   * @param target the target of the effect
   * @param duration the duration of the effect in ticks
   * @param type the type of bending effect that is being applied
   * @return the event after it was posted
   */
  public TickEffectEvent postTickEffectEvent(User source, Entity target, int duration, BendingEffect type) {
    TickEffectEvent event = new TickEffectEvent(source, target, duration, type);
    post(event);
    return event;
  }

  /**
   * Posts a new {@link BendingDamageEvent}.
   * @param source the user applying the damage
   * @param desc the ability that causes the damage
   * @param target the entity to be damaged
   * @param damage the amount of damage
   * @return the event after it was posted
   */
  public BendingDamageEvent postAbilityDamageEvent(User source, AbilityDescription desc, LivingEntity target, double damage) {
    BendingDamageEvent event = new BendingDamageEvent(source, desc, target, damage);
    post(event);
    return event;
  }

  /**
   * Posts a new {@link BendingExplosionEvent}.
   * @param source the user causing the explosion
   * @param center the epicenter of the explosion
   * @param blocks the blocks that are being exploded
   * @return the event after it was posted
   */
  public BendingExplosionEvent postExplosionEvent(User source, Vector3d center, Collection<Block> blocks) {
    BendingExplosionEvent event = new BendingExplosionEvent(source, center, blocks);
    post(event);
    return event;
  }

  /**
   * Posts a new {@link ActionLimitEvent}.
   * @param source the user responsible for the ActionLimiter
   * @param target the target entity to be affected by the ActionLimiter
   * @param duration the duration of the restriction in milliseconds
   * @return the event after it was posted
   */
  public ActionLimitEvent postActionLimitEvent(User source, LivingEntity target, long duration) {
    ActionLimitEvent event = new ActionLimitEvent(source, target, duration);
    post(event);
    return event;
  }

  /**
   * Posts a new {@link VelocityEvent}.
   * @param source the user responsible for the velocity change
   * @param target the target to be affected by the velocity change
   * @param desc the ability that causes the velocity change
   * @param velocity the new velocity to be applied
   * @return the event after it was posted
   */
  public VelocityEvent postVelocityEvent(User source, LivingEntity target, AbilityDescription desc, Vector3d velocity) {
    VelocityEvent event = new VelocityEvent(source, target, desc, velocity);
    post(event);
    return event;
  }
}
