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

package me.moros.bending.api.config.attribute;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Represents a mutable collection of {@link AttributeModifier}.
 */
public sealed interface AttributeHolder permits AttributeMap {
  /**
   * Add the specified attribute modifier to this instance.
   * @param policy the modifier policy
   * @param attribute the attribute for this modifier
   * @param modifier the modifier
   * @return true if modifier was successfully added, false otherwise
   */
  boolean add(ModifyPolicy policy, Attribute attribute, Modifier modifier);

  /**
   * Remove any attribute modifiers matching the specified predicate.
   * @param predicate the predicate to check against
   * @return true if any modifier was successfully removed, false otherwise
   */
  boolean remove(Predicate<AttributeModifier> predicate);

  /**
   * Clear all attribute modifiers of this instance.
   */
  void clear();

  /**
   * Stream all attribute modifiers this instance holds.
   * @return a stream of this instance's attribute modifiers
   */
  Stream<AttributeModifier> stream();

  static AttributeHolder createEmpty() {
    return new AttributeMap();
  }
}
