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

package me.moros.bending.model.temporal;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base implementation for registering and managing {@link Temporary}.
 * @param <K> the key type
 * @param <V> the value type
 */
public class TemporalManager<K, V extends TemporaryBase> {
  private final Consumer<V> consumer;
  private final Map<K, V> instances;
  private final TimerWheel wheel;
  private final String label;

  public TemporalManager(String name) {
    this(name, Temporary::revert);
  }

  public TemporalManager(String name, Consumer<V> consumer) {
    this.label = "Temporal " + name + " - tick";
    this.consumer = Objects.requireNonNull(consumer);
    this.instances = new ConcurrentHashMap<>();
    this.wheel = new TimerWheel();
  }

  public String label() {
    return label;
  }

  public void tick() {
    wheel.advance(Bukkit.getCurrentTick());
  }

  public boolean isTemp(@Nullable K key) {
    return key != null && instances.containsKey(key);
  }

  public Optional<V> get(K key) {
    return Optional.ofNullable(instances.get(key));
  }

  public void addEntry(K key, V value, int tickDuration) {
    if (isTemp(key)) {
      return;
    }
    instances.put(key, value);
    reschedule(key, tickDuration);
  }

  public void reschedule(K key, int tickDuration) {
    V value = instances.get(key);
    if (value != null) {
      int currentTick = Bukkit.getCurrentTick();
      value.expirationTick(currentTick + tickDuration);
      wheel.reschedule(value, currentTick);
    }
  }

  /**
   * This is used inside {@link Temporary#revert}
   * @param key the key of the entry to remove
   */
  public boolean removeEntry(K key) {
    V result = instances.remove(key);
    if (result != null) {
      wheel.deschedule(result);
      return true;
    }
    return false;
  }

  public void removeAll() {
    List.copyOf(instances.values()).forEach(consumer);
    clear();
  }

  protected void clear() {
    instances.clear();
  }
}
