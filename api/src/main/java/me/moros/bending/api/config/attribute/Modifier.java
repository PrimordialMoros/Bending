/*
 * Copyright 2020-2026 Moros
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

import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

public sealed interface Modifier extends DoubleUnaryOperator permits ModifierImpl {
  Modifier merge(Modifier other);

  AttributeModifier asAttributeModifier(ModifyPolicy policy, Attribute attribute);

  static Modifier of(ModifierOperation type, double value) {
    Objects.requireNonNull(type);
    if (!Double.isFinite(value)) {
      throw new IllegalArgumentException("Invalid value " + value);
    }
    return switch (type) {
      case ADDITIVE -> new ModifierImpl(value, 0, 1);
      case SUMMED_MULTIPLICATIVE -> new ModifierImpl(0, value, 1);
      case MULTIPLICATIVE -> new ModifierImpl(0, 0, value);
    };
  }
}
