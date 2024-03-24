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

package me.moros.bending.api.ability;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import me.moros.bending.api.ability.state.State;

/**
 * Wraps a collection of {@link Updatable}.
 */
public class MultiUpdatable<T extends Updatable> implements Updatable, Iterable<T> {
  protected final Collection<T> actions;

  MultiUpdatable(Collection<T> actions) {
    this.actions = new ArrayList<>(actions);
  }

  @Override
  public UpdateResult update() {
    actions.removeIf(action -> action.update() == UpdateResult.REMOVE);
    return actions.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  public boolean add(T action) {
    return actions.add(action);
  }

  public boolean removeIf(Predicate<? super T> filter) {
    return actions.removeIf(filter);
  }

  public Stream<T> stream() {
    return actions.stream();
  }

  public boolean isEmpty() {
    return actions.isEmpty();
  }

  public int size() {
    return actions.size();
  }

  public void clear() {
    actions.clear();
  }

  public State asState() {
    return State.from(this);
  }

  @Override
  public Iterator<T> iterator() {
    return Collections.unmodifiableCollection(actions).iterator();
  }

  public static <T extends Updatable> Builder<T> builder() {
    return new Builder<>();
  }

  public static <T extends Updatable> MultiUpdatable<T> empty() {
    return new Builder<T>().build();
  }

  public static class Builder<T extends Updatable> {
    private final Collection<T> actions;

    private Builder() {
      this.actions = new ArrayList<>();
    }

    public Builder<T> add(T action) {
      this.actions.add(action);
      return this;
    }

    public Builder<T> addAll(Iterable<T> actions) {
      for (T action : actions) {
        this.actions.add(action);
      }
      return this;
    }

    public MultiUpdatable<T> build() {
      return new MultiUpdatable<>(actions);
    }
  }
}
