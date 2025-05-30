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

package me.moros.bending.api.config;

import java.io.Serializable;
import java.util.List;

import me.moros.bending.api.ability.Ability;

/**
 * This is an interface that defines a serializable config.
 */
public interface Configurable extends Serializable {
  /**
   * Provides the path that serves as the root node for this configuration when serialized.
   * @return the path of this configuration's root node.
   */
  List<String> path();

  /**
   * Controls if this configuration is external and cannot be loaded from the main configuration file.
   * @return whether this configuration is external
   * @see ConfigProcessor#calculate(Ability, Class)
   */
  @Deprecated(forRemoval = true, since = "3.12.0")
  default boolean external() {
    return false;
  }
}
