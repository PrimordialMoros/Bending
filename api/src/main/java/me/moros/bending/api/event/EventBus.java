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

package me.moros.bending.api.event;

import java.util.Collection;
import java.util.function.Consumer;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.event.ElementChangeEvent.ElementAction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.data.DataKey;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The event bus is responsible for posting bending events.
 */
public interface EventBus {
  /**
   * Close this event bus preventing any further events from being posted.
   */
  void shutdown();

  /**
   * Registers the given subscriber to receive events.
   * @param event the event type
   * @param subscriber the subscriber
   * @param <T> the event type
   * @see #subscribe(Class, Consumer, int)
   */
  default <T extends BendingEvent> void subscribe(Class<T> event, Consumer<? super T> subscriber) {
    subscribe(event, subscriber, 0);
  }

  /**
   * Registers the given subscriber to receive events.
   * @param event the event type
   * @param subscriber the subscriber
   * @param priority the subscriber's priority, default priority is 0
   * @param <T> the event type
   */
  <T extends BendingEvent> void subscribe(Class<T> event, Consumer<? super T> subscriber, int priority);

  /**
   * Post an event.
   * @param event the event to post
   * @param <T> the type of event
   * @return true if the event was successfully posted, false otherwise
   */
  <T extends BendingEvent> boolean post(T event);

  /**
   * Posts a new {@link RegistryLockEvent}.
   * @param keys the RegistryKeys of all Registries that are going to be locked
   */
  void postRegistryLockEvent(Collection<DataKey<?>> keys);

  /**
   * Posts a new {@link UserRegisterEvent}.
   * @param user the user that was registered
   */
  void postUserRegisterEvent(User user);

  /**
   * Posts a new {@link CooldownChangeEvent.Add}.
   * @param user the relevant user
   * @param desc the ability to go on cooldown
   * @param duration the duration of the cooldown in milliseconds
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  boolean postCooldownAddEvent(User user, AbilityDescription desc, long duration);

  /**
   * Posts a new {@link CooldownChangeEvent.Remove}.
   * @param user the relevant user
   * @param desc the ability whose cooldown has expired
   */
  void postCooldownRemoveEvent(User user, AbilityDescription desc);

  /**
   * Posts a new {@link AbilityActivationEvent}.
   * @param user the user who attempts to activate the ability
   * @param desc the ability that is being activated
   * @param method the method used to activate the ability
   */
  void postAbilityActivationEvent(User user, AbilityDescription desc, Activation method);

  /**
   * Posts a new {@link ElementChangeEvent}.
   * @param user the user who is changing elements
   * @param element the associated element
   * @param type the type of element change
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  boolean postElementChangeEvent(User user, Element element, ElementAction type);

  /**
   * Posts a new {@link BindChangeEvent.Single}.
   * @param user the user who is changing binds
   * @param slot the slot index that is changed
   * @param desc the ability that is bound or cleared (null)
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  boolean postSingleBindChangeEvent(User user, int slot, @Nullable AbilityDescription desc);

  /**
   * Posts a new {@link BindChangeEvent.Multi}.
   * @param user the user who is changing binds
   * @param preset the preset that is bound
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  boolean postMultiBindChangeEvent(User user, Preset preset);

  /**
   * Posts a new {@link PresetRegisterEvent}.
   * @param user the user that the registered preset will belong to
   * @param preset the preset that is being registered
   * @return true if the event was executed and was not cancelled, false otherwise
   */
  boolean postPresetRegisterEvent(User user, Preset preset);

  /**
   * Posts a new {@link TickEffectEvent}.
   * @param source the user responsible for applying the effect
   * @param target the target of the effect
   * @param duration the duration of the effect in ticks
   * @param type the type of bending effect that is being applied
   * @return the event after it was posted
   */
  TickEffectEvent postTickEffectEvent(User source, Entity target, int duration, BendingEffect type);

  /**
   * Posts a new {@link BendingDamageEvent}.
   * @param source the user applying the damage
   * @param desc the ability that causes the damage
   * @param target the entity to be damaged
   * @param damage the amount of damage
   * @return the event after it was posted
   */
  BendingDamageEvent postAbilityDamageEvent(User source, AbilityDescription desc, LivingEntity target, double damage);

  /**
   * Posts a new {@link BendingExplosionEvent}.
   * @param source the user causing the explosion
   * @param desc the ability causing the explosion
   * @param center the epicenter of the explosion
   * @param blocks the blocks that are being exploded
   * @return the event after it was posted
   */
  BendingExplosionEvent postExplosionEvent(User source, AbilityDescription desc, Vector3d center, Collection<Block> blocks);

  /**
   * Posts a new {@link ActionLimitEvent}.
   * @param source the user responsible for the ActionLimiter
   * @param target the target entity to be affected by the ActionLimiter
   * @param duration the duration of the restriction in milliseconds
   * @return the event after it was posted
   */
  ActionLimitEvent postActionLimitEvent(User source, LivingEntity target, long duration);

  /**
   * Posts a new {@link VelocityEvent}.
   * @param source the user responsible for the velocity change
   * @param target the target to be affected by the velocity change
   * @param desc the ability that causes the velocity change
   * @param velocity the new velocity to be applied
   * @return the event after it was posted
   */
  VelocityEvent postVelocityEvent(User source, LivingEntity target, AbilityDescription desc, Vector3d velocity);
}
