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

package me.moros.bending.api.platform.item;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public interface ArmorContents<T> extends Iterable<T> {
  T helmet();

  T chestplate();

  T leggings();

  T boots();

  default List<T> toList() {
    return List.of(helmet(), chestplate(), leggings(), boots());
  }

  @Override
  default Iterator<T> iterator() {
    return toList().iterator();
  }

  default <R> ArmorContents<R> map(Function<T, R> f) {
    return of(f.apply(helmet()), f.apply(chestplate()), f.apply(leggings()), f.apply(boots()));
  }

  static <T> ArmorContents<T> of(T helmet, T chestplate, T leggings, T boots) {
    return new ArmorContentsImpl<>(helmet, chestplate, leggings, boots);
  }
}
