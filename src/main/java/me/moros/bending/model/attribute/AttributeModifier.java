/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.attribute;

import org.checkerframework.checker.nullness.qual.NonNull;

public final class AttributeModifier {
  private final ModifyPolicy policy;
  private final Attribute attribute;
  private final ModifierOperation type;
  private final double value;

  public AttributeModifier(@NonNull ModifyPolicy policy, @NonNull Attribute attribute, @NonNull ModifierOperation type, double value) {
    this.policy = policy;
    this.attribute = attribute;
    this.type = type;
    this.value = value;
  }

  public @NonNull ModifyPolicy policy() {
    return policy;
  }

  public @NonNull Attribute attribute() {
    return attribute;
  }

  public @NonNull ModifierOperation type() {
    return type;
  }

  public double value() {
    return value;
  }
}
