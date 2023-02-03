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

package me.moros.bending.api.platform.property;

public abstract sealed class Property<T extends Comparable<T>> permits IntegerProperty, BooleanProperty {
  private final String name;
  private final Class<T> type;

  protected Property(String name, Class<T> type) {
    this.name = name;
    this.type = type;
  }

  public String name() {
    return name;
  }

  public Class<T> type() {
    return type;
  }
}

