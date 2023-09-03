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

package me.moros.bending.common.event;

import java.util.Collection;
import java.util.function.Consumer;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.event.ActionLimitEvent;
import me.moros.bending.api.event.BendingDamageEvent;
import me.moros.bending.api.event.BendingEvent;
import me.moros.bending.api.event.BendingExplosionEvent;
import me.moros.bending.api.event.ElementChangeEvent.ElementAction;
import me.moros.bending.api.event.EventBus;
import me.moros.bending.api.event.TickEffectEvent;
import me.moros.bending.api.event.VelocityEvent;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.math.Vector3d;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;


public class EventBusImpl implements EventBus {
  private final net.kyori.event.EventBus<BendingEvent> eventBus;
  private boolean closed = false;

  public EventBusImpl() {
    this.eventBus = net.kyori.event.EventBus.create(BendingEvent.class);
  }

  @Override
  public void shutdown() {
    this.eventBus.unsubscribeIf(x -> true);
    this.closed = true;
  }

  @Override
  public <T extends BendingEvent> void subscribe(Class<T> event, Consumer<? super T> subscriber, int priority) {
    if (!closed) {
      eventBus.subscribe(event, new EventSubscriberImpl<>(subscriber, priority));
    }
  }

  @Override
  public <T extends BendingEvent> boolean post(T event) {
    if (closed) {
      throw new IllegalStateException("Eventbus has been terminated, cannot post new events!");
    }
    return eventBus.post(event).wasSuccessful();
  }

  @Override
  public void postRegistryLockEvent(Collection<Key> keys) {
    post(new RegistryLockEventImpl(keys));
  }

  @Override
  public void postUserRegisterEvent(User user) {
    post(new UserRegisterEventImpl(user));
  }

  @Override
  public boolean postCooldownAddEvent(User user, AbilityDescription desc, long duration) {
    return post(new CooldownChangeEventImpl.Add(user, desc, duration));
  }

  @Override
  public void postCooldownRemoveEvent(User user, AbilityDescription desc) {
    post(new CooldownChangeEventImpl.Remove(user, desc));
  }

  @Override
  public void postAbilityActivationEvent(User user, AbilityDescription desc, Activation method) {
    post(new AbilityActivationEventImpl(user, desc, method));
  }

  @Override
  public boolean postElementChangeEvent(User user, Element element, ElementAction type) {
    return post(new ElementChangeEventImpl(user, element, type));
  }

  @Override
  public boolean postSingleBindChangeEvent(User user, int slot, @Nullable AbilityDescription desc) {
    return post(new BindChangeEventImpl.Single(user, slot, desc));
  }

  @Override
  public boolean postMultiBindChangeEvent(User user, Preset preset) {
    return post(new BindChangeEventImpl.Multi(user, preset));
  }

  @Override
  public boolean postPresetRegisterEvent(User user, Preset preset) {
    return !preset.isEmpty() && post(new PresetRegisterEventImpl(user, preset));
  }

  @Override
  public TickEffectEvent postTickEffectEvent(User source, Entity target, int duration, BendingEffect type) {
    TickEffectEvent event = new TickEffectEventImpl(source, target, duration, type);
    post(event);
    return event;
  }

  @Override
  public BendingDamageEvent postAbilityDamageEvent(User source, AbilityDescription desc, LivingEntity target, double damage) {
    BendingDamageEvent event = new BendingDamageEventImpl(source, desc, target, damage);
    post(event);
    return event;
  }

  @Override
  public BendingExplosionEvent postExplosionEvent(User source, AbilityDescription desc, Vector3d center, Collection<Block> blocks) {
    BendingExplosionEvent event = new BendingExplosionEventImpl(source, desc, center, blocks);
    post(event);
    return event;
  }

  @Override
  public ActionLimitEvent postActionLimitEvent(User source, LivingEntity target, long duration) {
    ActionLimitEvent event = new ActionLimitEventImpl(source, target, duration);
    post(event);
    return event;
  }

  @Override
  public VelocityEvent postVelocityEvent(User source, LivingEntity target, AbilityDescription desc, Vector3d velocity) {
    VelocityEventImpl event = new VelocityEventImpl(source, target, desc, velocity);
    post(event);
    return event;
  }
}
