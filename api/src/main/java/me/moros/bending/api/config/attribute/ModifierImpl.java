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

package me.moros.bending.api.config.attribute;

import java.util.Objects;

record ModifierImpl(double additive, double summedMultiplicative, double multiplicative) implements Modifier {
  @Override
  public Modifier merge(Modifier other) {
    ModifierImpl otherModifier = (ModifierImpl) other;
    double additive = this.additive + otherModifier.additive;
    double summedMultiplicative = this.summedMultiplicative + otherModifier.summedMultiplicative;
    double multiplicative = this.multiplicative * otherModifier.multiplicative;
    return new ModifierImpl(additive, summedMultiplicative, multiplicative);
  }

  @Override
  public AttributeModifier asAttributeModifier(ModifyPolicy policy, Attribute attribute) {
    Objects.requireNonNull(policy);
    Objects.requireNonNull(attribute);
    return new AttributeModifierImpl(policy, attribute, this);
  }

  @Override
  public double applyAsDouble(double value) {
    return (value + additive) * (1 + summedMultiplicative) * multiplicative;
  }
}
