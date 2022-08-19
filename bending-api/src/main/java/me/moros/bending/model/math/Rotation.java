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

package me.moros.bending.model.math;

/**
 * Immutable implementation of rotation in 3D space.
 */
public final class Rotation {
  private final double q0;
  private final double q1;
  private final double q2;
  private final double q3;

  private Rotation(double q0, double q1, double q2, double q3) {
    this.q0 = q0;
    this.q1 = q1;
    this.q2 = q2;
    this.q3 = q3;
  }

  public Rotation(Vector3d axis, double angle) throws IllegalArgumentException {
    double norm = axis.length();
    if (norm == 0) {
      throw new IllegalArgumentException();
    }

    double halfAngle = -0.5 * angle;
    double coeff = Math.sin(halfAngle) / norm;

    q0 = Math.cos(halfAngle);
    q1 = coeff * axis.x();
    q2 = coeff * axis.y();
    q3 = coeff * axis.z();
  }

  /**
   * @return the 3x3 matrix corresponding to the instance
   */
  public double[][] getMatrix() {
    // products
    double q0q0 = q0 * q0;
    double q0q1 = q0 * q1;
    double q0q2 = q0 * q2;
    double q0q3 = q0 * q3;
    double q1q1 = q1 * q1;
    double q1q2 = q1 * q2;
    double q1q3 = q1 * q3;
    double q2q2 = q2 * q2;
    double q2q3 = q2 * q3;
    double q3q3 = q3 * q3;
    // create the matrix
    double[][] m = new double[3][];
    m[0] = new double[3];
    m[1] = new double[3];
    m[2] = new double[3];

    m[0][0] = 2.0 * (q0q0 + q1q1) - 1.0;
    m[1][0] = 2.0 * (q1q2 - q0q3);
    m[2][0] = 2.0 * (q1q3 + q0q2);

    m[0][1] = 2.0 * (q1q2 + q0q3);
    m[1][1] = 2.0 * (q0q0 + q2q2) - 1.0;
    m[2][1] = 2.0 * (q2q3 - q0q1);

    m[0][2] = 2.0 * (q1q3 - q0q2);
    m[1][2] = 2.0 * (q2q3 + q0q1);
    m[2][2] = 2.0 * (q0q0 + q3q3) - 1.0;
    return m;
  }

  /**
   * Apply the rotation to a vector.
   * @param u vector to apply the rotation to
   * @return a new vector which is the image of u by the rotation
   */
  public Vector3d applyTo(Vector3d u) {
    double x = u.x();
    double y = u.y();
    double z = u.z();
    double s = q1 * x + q2 * y + q3 * z;
    return new Vector3d(2 * (q0 * (x * q0 - (q2 * z - q3 * y)) + s * q1) - x,
      2 * (q0 * (y * q0 - (q3 * x - q1 * z)) + s * q2) - y,
      2 * (q0 * (z * q0 - (q1 * y - q2 * x)) + s * q3) - z);
  }

  /**
   * Apply the rotation to a vector stored in an array.
   * @param in an array with three items which stores vector to rotate
   * @param out an array with three items to put result to (it can be the same array as in)
   */
  public void applyTo(final double[] in, final double[] out) {
    final double x = in[0];
    final double y = in[1];
    final double z = in[2];

    final double s = q1 * x + q2 * y + q3 * z;

    out[0] = 2 * (q0 * (x * q0 - (q2 * z - q3 * y)) + s * q1) - x;
    out[1] = 2 * (q0 * (y * q0 - (q3 * x - q1 * z)) + s * q2) - y;
    out[2] = 2 * (q0 * (z * q0 - (q1 * y - q2 * x)) + s * q3) - z;
  }

  /**
   * Apply the inverse of the rotation to a vector.
   * @param u vector to apply the inverse of the rotation to
   * @return a new vector which such that u is its image by the rotation
   */
  public Vector3d applyInverseTo(Vector3d u) {
    double x = u.x();
    double y = u.y();
    double z = u.z();

    double s = q1 * x + q2 * y + q3 * z;
    double m0 = -q0;

    return new Vector3d(2 * (m0 * (x * m0 - (q2 * z - q3 * y)) + s * q1) - x,
      2 * (m0 * (y * m0 - (q3 * x - q1 * z)) + s * q2) - y,
      2 * (m0 * (z * m0 - (q1 * y - q2 * x)) + s * q3) - z);
  }

  /**
   * Apply the inverse of the rotation to a vector stored in an array.
   * @param in an array with three items which stores vector to rotate
   * @param out an array with three items to put result to (it can be the same array as in)
   */
  public void applyInverseTo(final double[] in, final double[] out) {
    final double x = in[0];
    final double y = in[1];
    final double z = in[2];

    final double s = q1 * x + q2 * y + q3 * z;
    final double m0 = -q0;

    out[0] = 2 * (m0 * (x * m0 - (q2 * z - q3 * y)) + s * q1) - x;
    out[1] = 2 * (m0 * (y * m0 - (q3 * x - q1 * z)) + s * q2) - y;
    out[2] = 2 * (m0 * (z * m0 - (q1 * y - q2 * x)) + s * q3) - z;
  }

  /**
   * Apply the instance to another rotation.
   * @param r rotation to apply the rotation to
   * @return a new rotation which is the composition of r by the instance
   */
  public Rotation applyTo(Rotation r) {
    return new Rotation(r.q0 * q0 - (r.q1 * q1 + r.q2 * q2 + r.q3 * q3),
      r.q1 * q0 + r.q0 * q1 + (r.q2 * q3 - r.q3 * q2),
      r.q2 * q0 + r.q0 * q2 + (r.q3 * q1 - r.q1 * q3),
      r.q3 * q0 + r.q0 * q3 + (r.q1 * q2 - r.q2 * q1));
  }

  /**
   * Apply the inverse of the instance to another rotation.
   * @param r rotation to apply the rotation to
   * @return a new rotation which is the composition of r by the inverse of the instance
   */
  public Rotation applyInverseTo(Rotation r) {
    return new Rotation(-r.q0 * q0 - (r.q1 * q1 + r.q2 * q2 + r.q3 * q3),
      -r.q1 * q0 + r.q0 * q1 + (r.q2 * q3 - r.q3 * q2),
      -r.q2 * q0 + r.q0 * q2 + (r.q3 * q1 - r.q1 * q3),
      -r.q3 * q0 + r.q0 * q3 + (r.q1 * q2 - r.q2 * q1));
  }
}
