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

package me.moros.bending.model.user;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DataHolder {
  <E> boolean containsKey(@NonNull DataKey<E> key);

  <E> boolean canEdit(@NonNull DataKey<E> key);

  <E> boolean offer(@NonNull DataKey<E> key, @NonNull E value);

  <E> @Nullable E remove(@NonNull DataKey<E> key);

  <E> @Nullable E get(@NonNull DataKey<E> key);

  <E> @NonNull E getOrDefault(@NonNull DataKey<E> key, @NonNull E defaultValue);

  <E extends Enum<E>> @NonNull E toggle(@NonNull DataKey<E> key, @NonNull E defaultValue);
}
