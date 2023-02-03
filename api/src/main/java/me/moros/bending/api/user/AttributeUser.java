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

package me.moros.bending.api.user;

import java.util.stream.Stream;

import me.moros.bending.api.config.attribute.AttributeModifier;

/**
 * Represents a user that has a collection of {@link AttributeModifier}.
 */
public interface AttributeUser {
  /**
   * Add the specified attribute modifier to this instance.
   * @param modifier the modifier to add
   * @return true if modifier was successfully added, false otherwise
   */
  boolean addAttribute(AttributeModifier modifier);

  /**
   * Clear all attribute modifiers of this instance.
   */
  void clearAttributes();

  /**
   * Stream all attribute modifiers this instance holds.
   * @return a stream of this instance's attribute modifiers
   */
  Stream<AttributeModifier> attributes();
}
