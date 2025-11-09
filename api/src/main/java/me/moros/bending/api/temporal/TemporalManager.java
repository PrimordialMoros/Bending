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

package me.moros.bending.api.temporal;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import me.moros.bending.api.platform.Platform;
import me.moros.tasker.TimerWheel;
import org.jspecify.annotations.Nullable;

/**
 * Base implementation for registering and managing {@link Temporary}.
 * @param <K> the key type
 * @param <V> the value type
 */
public class TemporalManager<K, V extends Temporary> {
  private final TimerWheel wheel;
  private final Map<K, V> instances;
  private final boolean revertOnClear;
  private final AtomicBoolean clearing = new AtomicBoolean();
  private final int max;

  public TemporalManager(int wheelCapacity) {
    this(wheelCapacity, true);
  }

  public TemporalManager(int wheelCapacity, boolean revertOnClear) {
    this(TimerWheel.simple(wheelCapacity + 1), revertOnClear, wheelCapacity);
  }

  public TemporalManager(TimerWheel wheel) {
    this(wheel, true, Temporary.DEFAULT_REVERT);
  }

  private TemporalManager(TimerWheel wheel, boolean revertOnClear, int max) {
    this.wheel = Objects.requireNonNull(wheel);
    this.instances = new ConcurrentHashMap<>();
    this.revertOnClear = revertOnClear;
    this.max = max;
  }

  public void tick() {
    wheel.advance();
  }

  public boolean isTemp(@Nullable K key) {
    return key != null && instances.containsKey(key);
  }

  public Optional<V> get(K key) {
    return Optional.ofNullable(instances.get(key));
  }

  public void addEntry(K key, V value, int ticks) {
    if (isTemp(key) || ticks < 0) {
      return;
    }
    instances.put(key, value);
    wheel.schedule(value, ticks);
  }

  /**
   * This is used inside {@link Temporary#revert}
   * @param key the key of the entry to remove
   */
  public boolean removeEntry(K key) {
    V result = instances.remove(key);
    if (result != null) {
      result.cancel();
      return true;
    }
    return false;
  }

  public boolean clearing() {
    return clearing.get();
  }

  public void removeAll() {
    clearing.set(true);
    wheel.shutdown(revertOnClear);
    instances.clear();
    clearing.set(false);
  }

  public int fromMillis(long duration) {
    int time = Platform.instance().toTicks(duration, TimeUnit.MILLISECONDS);
    return time <= 0 ? max : Math.min(time, max);
  }

  public int size() {
    return instances.size();
  }

  protected Stream<V> stream() {
    return instances.values().stream();
  }
}
