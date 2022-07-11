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

import me.moros.bending.model.key.RegistryKey;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface DataHolder {
  <T> boolean containsKey(RegistryKey<T> key);

  <T> boolean canEdit(RegistryKey<T> key);

  <T> boolean offer(RegistryKey<T> key, T value);

  <T> @Nullable T remove(RegistryKey<T> key);

  <T> @Nullable T get(RegistryKey<T> key);

  <T> T getOrDefault(RegistryKey<T> key, T defaultValue);

  <T extends Enum<T>> T toggle(RegistryKey<T> key, T defaultValue);
}
