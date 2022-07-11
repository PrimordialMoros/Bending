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

package me.moros.bending.model.key;

public final class KeyImpl implements Key {
  private final String namespace;
  private final String value;

  KeyImpl(String namespace, String value) {
    this.namespace = namespace;
    this.value = value;
  }

  @Override
  public String namespace() {
    return namespace;
  }

  @Override
  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return namespace + '.' + value;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Key key) {
      return this.namespace.equals(key.namespace()) && this.value.equals(key.value());
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = namespace.hashCode();
    result = (31 * result) + value.hashCode();
    return result;
  }
}
