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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TemporalManager<K, V extends Temporary> {
  private final Consumer<V> consumer;
  private final Map<K, V> instances;
  private final TimerWheel<K, V> wheel;

  public TemporalManager() {
    this(Temporary::revert);
  }

  public TemporalManager(@NonNull Consumer<V> consumer) {
    this.consumer = Objects.requireNonNull(consumer);
    this.instances = new ConcurrentHashMap<>();
    this.wheel = new TimerWheel<>(this);
  }

  public void tick() {
    wheel.advance(Bukkit.getCurrentTick());
  }

  public boolean isTemp(@Nullable K key) {
    return key != null && instances.containsKey(key);
  }

  public Optional<V> get(@NonNull K key) {
    return Optional.ofNullable(instances.get(key));
  }

  public void addEntry(@NonNull K key, @NonNull V value, int tickDuration) {
    if (isTemp(key)) {
      return;
    }
    instances.put(key, value);
    reschedule(key, tickDuration);
  }

  public void reschedule(@NonNull K key, int tickDuration) {
    V value = instances.get(key);
    if (value != null) {
      Node<K, V> node = new Node<>(key, value);
      int currentTick = Bukkit.getCurrentTick();
      node.expirationTick(currentTick + tickDuration);
      wheel.reschedule(node, currentTick);
    }
  }

  /**
   * This is used inside {@link Temporary#revert}
   * @param key the key of the entry to remove
   */
  public boolean removeEntry(@NonNull K key) {
    V result = instances.remove(key);
    if (result != null) {
      wheel.deschedule(new Node<>(key, result));
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
