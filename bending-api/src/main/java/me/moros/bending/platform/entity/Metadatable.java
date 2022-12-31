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

package me.moros.bending.platform.entity;

import java.util.stream.Stream;

import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Metadatable {
  boolean hasMetadata(Key key);

  <T> Stream<T> metadata(Key key, Class<T> type);

  default void addMetadata(Key key) {
    addMetadata(key, null);
  }

  void addMetadata(Key key, @Nullable Object object);

  void removeMetadata(Key key);

  interface Persistent {
    boolean hasPersistentMetadata(Key key);

    boolean addPersistentMetadata(Key key);

    void removePersistentMetadata(Key key);
  }
}
