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

package me.moros.bending.model.attribute;

import java.util.function.DoubleFunction;

public enum AttributeConverter implements DoubleFunction<Number> {
  DOUBLE(x -> x),
  INT(x -> (int) x),
  LONG(x -> (long) x);

  private final DoubleFunction<Number> converter;

  AttributeConverter(DoubleFunction<Number> converter) {
    this.converter = converter;
  }

  @Override
  public Number apply(double value) {
    return converter.apply(value);
  }
}
