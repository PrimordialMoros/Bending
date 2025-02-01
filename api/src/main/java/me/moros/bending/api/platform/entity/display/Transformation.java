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

package me.moros.bending.api.platform.entity.display;

import me.moros.math.Position;
import me.moros.math.Quaternion;
import me.moros.math.Vector3d;

public record Transformation(Position translation, Quaternion left, Position scale, Quaternion right) {
  public Transformation(Position translation, Position scale) {
    this(translation, QuaternionRotation.ZERO, scale, QuaternionRotation.ZERO);
  }

  public Transformation withTranslation(Position translation) {
    return new Transformation(translation, left, scale, right);
  }

  public Transformation withScale(Position scale) {
    return new Transformation(translation, left, scale, right);
  }

  public Transformation withLeftRotation(Quaternion left) {
    return new Transformation(translation, left, scale, right);
  }

  public Transformation withRightRotation(Quaternion right) {
    return new Transformation(translation, left, scale, right);
  }

  public static Transformation scaled(double scale) {
    return new Transformation(Vector3d.ZERO, Vector3d.ONE.multiply(scale));
  }

  private record QuaternionRotation(double q0, double q1, double q2, double q3) implements Quaternion {
    private static final QuaternionRotation ZERO = new QuaternionRotation(1, 0, 0, 0);
  }
}

