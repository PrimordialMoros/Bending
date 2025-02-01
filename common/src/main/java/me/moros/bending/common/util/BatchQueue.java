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

package me.moros.bending.common.util;

import java.util.Collection;
import java.util.Queue;

public interface BatchQueue<E> {
  Queue<E> queue();

  default boolean isEmpty() {
    return queue().isEmpty();
  }

  default void clear() {
    queue().clear();
  }

  default boolean fillQueue(Collection<? extends E> elements) {
    return queue().addAll(elements);
  }

  default void processQueue(int amount) {
    int counter = 0;
    while (!queue().isEmpty() && counter <= amount) {
      if (process(queue().poll())) {
        counter++;
      }
    }
  }

  boolean process(E element);
}
