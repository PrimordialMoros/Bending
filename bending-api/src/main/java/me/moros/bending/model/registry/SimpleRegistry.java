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

package me.moros.bending.model.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Simple registry implementation.
 * @param <K> the key type
 * @param <V> the value type
 */
public class SimpleRegistry<K, V> implements Registry<K, V> {
  private final String namespace;

  protected final Map<K, V> registryMap;
  protected final Function<V, K> inverseMapper;
  protected final Function<String, K> keyMapper;

  protected boolean locked = false;

  protected SimpleRegistry(String namespace, Function<V, K> inverseMapper, Function<String, K> keyMapper) {
    registryMap = new ConcurrentHashMap<>();
    this.namespace = namespace;
    this.inverseMapper = inverseMapper;
    this.keyMapper = keyMapper;
  }

  private Collection<V> values() {
    return registryMap.values();
  }

  @Override
  public String namespace() {
    return namespace;
  }

  @Override
  public boolean register(V value) {
    checkLock();
    K key = inverseMapper.apply(value);
    return registryMap.putIfAbsent(key, value) == null;
  }

  @Override
  public void lock() {
    this.locked = true;
  }

  @Override
  public boolean isLocked() {
    return locked;
  }

  protected void checkLock() {
    if (isLocked()) {
      throw new RegistryModificationException("Registry is locked!");
    }
  }

  @Override
  public boolean containsKey(K key) {
    return registryMap.containsKey(key);
  }

  @Override
  public boolean containsValue(V value) {
    Objects.requireNonNull(value);
    return containsKey(inverseMapper.apply(value));
  }

  @Override
  public @Nullable V get(K key) {
    return registryMap.get(key);
  }

  @Override
  public @Nullable V fromString(String input) {
    Objects.requireNonNull(input);
    K key = keyMapper.apply(input);
    return key == null ? null : get(key);
  }

  @Override
  public int size() {
    return registryMap.size();
  }

  @Override
  public Stream<V> stream() {
    return values().stream();
  }

  @Override
  public Stream<K> streamKeys() {
    return registryMap.keySet().stream();
  }

  @Override
  public Set<K> keys() {
    return Set.copyOf(registryMap.keySet());
  }

  @Override
  public Iterator<V> iterator() {
    return Collections.unmodifiableCollection(values()).iterator();
  }

  public static class SimpleMutableRegistry<K, V> extends SimpleRegistry<K, V> implements MutableRegistry<K, V> {
    protected SimpleMutableRegistry(String namespace, Function<V, K> inverseMapper, Function<String, K> keyMapper) {
      super(namespace, inverseMapper, keyMapper);
    }

    @Override
    public boolean invalidateKey(K key) {
      checkLock();
      return registryMap.remove(key) != null;
    }

    @Override
    public boolean invalidateValue(V value) {
      return invalidateKey(inverseMapper.apply(value));
    }

    @Override
    public boolean clear() {
      checkLock();
      registryMap.clear();
      return true;
    }
  }
}
