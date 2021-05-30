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

package me.moros.bending.model.math;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Immutable 3D Vector implementation with integer coordinates
 */
public class IntVector {
  public static final IntVector ZERO = new IntVector(0, 0, 0);

  public final int x;
  public final int y;
  public final int z;

  /**
   * Build a vector from its coordinates.
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   */
  public IntVector(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public IntVector(@NonNull Block b) {
    this(b.getX(), b.getY(), b.getZ());
  }

  /**
   * Build a vector from its coordinates.
   * @param v coordinates array
   * @throws IllegalArgumentException if array does not have 3 elements
   * @see #toArray()
   */
  public IntVector(int[] v) throws IllegalArgumentException {
    if (v.length != 3) {
      throw new IllegalArgumentException();
    }
    this.x = v[0];
    this.y = v[1];
    this.z = v[2];
  }

  /**
   * Get the vector coordinates as a dimension 3 array.
   * @return vector coordinates
   * @see #IntVector(int[])
   */
  public int[] toArray() {
    return new int[]{x, y, z};
  }

  /**
   * @return Euclidean norm for the vector
   */
  public double getNorm() {
    return Math.sqrt(x * x + y * y + z * z);
  }

  /**
   * @return square of the Euclidean norm for the vector
   */
  public int getNormSq() {
    return x * x + y * y + z * z;
  }

  /**
   * Add a vector to the instance.
   * @param v vector to add
   * @return a new vector
   */
  public @NonNull IntVector add(@NonNull IntVector v) {
    return new IntVector(x + v.x, y + v.y, z + v.z);
  }

  /**
   * Subtract a vector from the instance.
   * @param v vector to subtract
   * @return a new vector
   */
  public @NonNull IntVector subtract(@NonNull IntVector v) {
    return new IntVector(x - v.x, y - v.y, z - v.z);
  }

  /**
   * Get the opposite of the instance.
   * @return a new vector which is opposite to the instance
   */
  public @NonNull IntVector negate() {
    return new IntVector(-x, -y, -z);
  }

  /**
   * Multiply the instance by a scalar.
   * @param a scalar
   * @return a new vector
   */
  public @NonNull IntVector multiply(int a) {
    return new IntVector(a * x, a * y, a * z);
  }

  public @NonNull IntVector multiply(@NonNull IntVector v) {
    return new IntVector(x * v.x, y * v.y, z * v.z);
  }

  public int dotProduct(@NonNull IntVector v) {
    return x * v.x + y * v.y + z * v.z;
  }

  public @NonNull IntVector crossProduct(@NonNull IntVector v) {
    int newX = y * v.z - v.y * z;
    int newY = z * v.x - v.z * x;
    int newZ = x * v.y - v.x * y;
    return new IntVector(newX, newY, newZ);
  }

  public double distance(@NonNull IntVector v) {
    int dx = v.x - x;
    int dy = v.y - y;
    int dz = v.z - z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  public int distanceSq(@NonNull IntVector v) {
    int dx = v.x - x;
    int dy = v.y - y;
    int dz = v.z - z;
    return dx * dx + dy * dy + dz * dz;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof IntVector) {
      final IntVector v = (IntVector) other;
      return (x == v.x) && (y == v.y) && (z == v.z);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int result = x;
    result = 31 * result + y;
    result = 31 * result + z;
    return result;
  }

  @Override
  public String toString() {
    return "[" + x + ", " + y + ", " + z + "]";
  }

  public @NonNull Block toBlock(@NonNull World world) {
    return world.getBlockAt(x, y, z);
  }
}
