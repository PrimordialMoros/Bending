/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.config.processor;

import java.util.function.DoubleUnaryOperator;

import me.moros.bending.api.config.attribute.AttributeModifier;

public record ModificationMatrix(double add, double summedMult, double mult) implements DoubleUnaryOperator {
  public static ModificationMatrix from(AttributeModifier modifier) {
    return switch (modifier.type()) {
      case ADDITIVE -> new ModificationMatrix(modifier.value(), 0, 1);
      case SUMMED_MULTIPLICATIVE -> new ModificationMatrix(0, modifier.value(), 1);
      case MULTIPLICATIVE -> new ModificationMatrix(0, 0, modifier.value());
    };
  }

  public ModificationMatrix merge(ModificationMatrix other) {
    return new ModificationMatrix(add + other.add, summedMult + other.summedMult, mult * other.mult);
  }

  @Override
  public double applyAsDouble(double value) {
    return (value + add) * (1 + summedMult) * mult;
  }
}
