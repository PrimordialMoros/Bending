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

import java.util.Optional;

import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;

public final class DummyDataHolder implements DataHolder {
  public static final DataHolder INSTANCE = new DummyDataHolder();

  private DummyDataHolder() {
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return Optional.empty();
  }

  @Override
  public <T> void add(DataKey<T> key, T value) {
  }

  @Override
  public <T> void remove(DataKey<T> key) {
  }
}
