/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.registry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TagBuilder<V extends Keyed, R extends Tag<V>> {
  private final Function<Container<V>, R> function;
  private final Key key;
  private final Registry<?, V> registry;
  private final Set<V> container;

  public TagBuilder(Key key, Registry<?, V> registry, Function<Container<V>, R> function) {
    this.key = key;
    this.registry = registry;
    this.function = function;
    container = new HashSet<>();
  }

  public TagBuilder<V, R> startsWith(String with) {
    return add(value -> name(value).startsWith(with));
  }

  public TagBuilder<V, R> contains(String with) {
    return add(value -> name(value).contains(with));
  }

  public TagBuilder<V, R> endsWith(String with) {
    return add(value -> name(value).endsWith(with));
  }

  public TagBuilder<V, R> add(V type) {
    this.container.add(type);
    return this;
  }

  @SafeVarargs
  public final TagBuilder<V, R> add(V type, V @Nullable ... other) {
    this.container.add(type);
    if (other != null) {
      this.container.addAll(List.of(other));
    }
    return this;
  }

  public TagBuilder<V, R> add(Iterable<V> values) {
    for (V type : values) {
      this.container.add(type);
    }
    return this;
  }

  public TagBuilder<V, R> add(Predicate<V> predicate) {
    for (V type : registry) {
      if (predicate.test(type)) {
        this.container.add(type);
      }
    }
    return this;
  }

  public TagBuilder<V, R> notStartsWith(String with) {
    return not(value -> name(value).endsWith(with));
  }

  public TagBuilder<V, R> notContains(String with) {
    return not(value -> name(value).contains(with));
  }

  public TagBuilder<V, R> notEndsWith(String with) {
    return not(value -> name(value).endsWith(with));
  }

  public TagBuilder<V, R> not(V type) {
    this.container.remove(type);
    return this;
  }

  @SafeVarargs
  public final TagBuilder<V, R> not(V type, V @Nullable ... other) {
    this.container.remove(type);
    if (other != null) {
      for (V val : other) {
        this.container.remove(val);
      }
    }
    return this;
  }

  public TagBuilder<V, R> not(Iterable<V> values) {
    for (V type : values) {
      this.container.remove(type);
    }
    return this;
  }

  public TagBuilder<V, R> not(Predicate<V> predicate) {
    for (V type : registry) {
      if (predicate.test(type)) {
        this.container.remove(type);
      }
    }
    return this;
  }

  public Container<V> buildContainer() {
    return Container.create(key, container);
  }

  public R build() {
    return function.apply(buildContainer());
  }

  public R buildAndRegister() {
    var tag = build();
    registry.registerTag(tag);
    return tag;
  }

  private static String name(Keyed key) {
    return key.key().value();
  }
}
