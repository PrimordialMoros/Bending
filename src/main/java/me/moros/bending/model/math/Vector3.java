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

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Immutable 3D Vector implementation
 */
public class Vector3 {
  public static final Vector3 ZERO = new Vector3(0, 0, 0);
  public static final Vector3 ONE = new Vector3(1, 1, 1);
  public static final Vector3 PLUS_I = new Vector3(1, 0, 0);
  public static final Vector3 MINUS_I = new Vector3(-1, 0, 0);
  public static final Vector3 PLUS_J = new Vector3(0, 1, 0);
  public static final Vector3 MINUS_J = new Vector3(0, -1, 0);
  public static final Vector3 PLUS_K = new Vector3(0, 0, 1);
  public static final Vector3 MINUS_K = new Vector3(0, 0, -1);

  /**
   * Min and max bukkit velocity vectors
   */
  public static final Vector3 MIN_VELOCITY = new Vector3(-4, -4, -4);
  public static final Vector3 MAX_VELOCITY = new Vector3(4, 4, 4);

  public final double x;
  public final double y;
  public final double z;

  /**
   * Build a vector from its coordinates
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   */
  public Vector3(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * Build a vector from its coordinates
   * @param v coordinates array
   * @throws IllegalArgumentException if array does not have 3 elements
   * @see #toArray()
   */
  public Vector3(double[] v) throws IllegalArgumentException {
    if (v.length != 3) {
      throw new IllegalArgumentException("Expected array length " + 3 + " found " + v.length);
    }
    this.x = v[0];
    this.y = v[1];
    this.z = v[2];
  }

  public Vector3(@NonNull IntVector v) {
    this(v.x, v.y, v.z);
  }

  public Vector3(org.bukkit.util.@NonNull Vector v) {
    this(v.getX(), v.getY(), v.getZ());
  }

  public Vector3(@NonNull Location l) {
    this(l.getX(), l.getY(), l.getZ());
  }

  public Vector3(@NonNull Block b) {
    this(b.getX(), b.getY(), b.getZ());
  }

  /**
   * Get the vector coordinates as a dimension 3 array.
   * @return vector coordinates
   * @see #Vector3(double[])
   */
  public double[] toArray() {
    return new double[]{x, y, z};
  }

  /**
   * @return Euclidean norm for the vector
   */
  public double getNorm() {
    // there are no cancellation problems here, so we use the straightforward formula
    return Math.sqrt(x * x + y * y + z * z);
  }

  /**
   * @return square of the Euclidean norm for the vector
   */
  public double getNormSq() {
    // there are no cancellation problems here, so we use the straightforward formula
    return x * x + y * y + z * z;
  }

  /**
   * Add a vector to the instance.
   * @param v vector to add
   * @return a new vector
   */
  public @NonNull Vector3 add(@NonNull Vector3 v) {
    return new Vector3(x + v.x, y + v.y, z + v.z);
  }

  /**
   * Subtract a vector from the instance.
   * @param v vector to subtract
   * @return a new vector
   */
  public @NonNull Vector3 subtract(@NonNull Vector3 v) {
    return new Vector3(x - v.x, y - v.y, z - v.z);
  }

  /**
   * @return {@link #normalize(Vector3)} with {@link #PLUS_I} as default.
   */
  public @NonNull Vector3 normalize() {
    return normalize(Vector3.PLUS_I);
  }

  /**
   * Get a normalized vector aligned with the instance.
   * @param def the default vector to return if norm is zero
   * @return a new normalized vector
   */
  public @NonNull Vector3 normalize(@NonNull Vector3 def) {
    double s = getNorm();
    if (s == 0) {
      return def;
    }
    return multiply(1 / s);
  }

  /**
   * Get the opposite of the instance.
   * @return a new vector which is opposite to the instance
   */
  public @NonNull Vector3 negate() {
    return new Vector3(-x, -y, -z);
  }

  /**
   * Multiply the instance by a scalar.
   * @param a scalar
   * @return a new vector
   */
  public @NonNull Vector3 multiply(double a) {
    return new Vector3(a * x, a * y, a * z);
  }

  public @NonNull Vector3 multiply(@NonNull Vector3 v) {
    return new Vector3(x * v.x, y * v.y, z * v.z);
  }

  /**
   * @return true if any coordinate of this point is NaN; false otherwise
   */
  public boolean isNaN() {
    return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
  }

  /**
   * @return true if any coordinate of this vector is infinite and none are NaN false otherwise
   */
  public boolean isInfinite() {
    return !isNaN() && (Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z));
  }

  public double dotProduct(@NonNull Vector3 v) {
    return x * v.x + y * v.y + z * v.z;
  }

  public @NonNull Vector3 crossProduct(@NonNull Vector3 v) {
    double newX = y * v.z - v.y * z;
    double newY = z * v.x - v.z * x;
    double newZ = x * v.y - v.x * y;
    return new Vector3(newX, newY, newZ);
  }

  public double distance(@NonNull Vector3 v) {
    double dx = v.x - x;
    double dy = v.y - y;
    double dz = v.z - z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  public double distanceSq(@NonNull Vector3 v) {
    double dx = v.x - x;
    double dy = v.y - y;
    double dz = v.z - z;
    return dx * dx + dy * dy + dz * dz;
  }

  /**
   * Compute the angular separation between this and another vector.
   * @param v the other vector
   * @return angular separation between this and v or zero if either vector has a null norm
   */
  public double angle(@NonNull Vector3 v) {
    double normProduct = getNorm() * v.getNorm();
    if (normProduct == 0) {
      return 0;
    }
    double dot = Math.min(Math.max(dotProduct(v) / normProduct, -1), 1);
    return Math.acos(dot);
  }

  public @NonNull Vector3 setX(double value) {
    return new Vector3(value, y, z);
  }

  public @NonNull Vector3 setY(double value) {
    return new Vector3(x, value, z);
  }

  public @NonNull Vector3 setZ(double value) {
    return new Vector3(x, y, value);
  }

  public @NonNull Vector3 min(@NonNull Vector3 v) {
    return new Vector3(Math.min(x, v.x), Math.min(y, v.y), Math.min(z, v.z));
  }

  public @NonNull Vector3 max(@NonNull Vector3 v) {
    return new Vector3(Math.max(x, v.x), Math.max(y, v.y), Math.max(z, v.z));
  }

  public @NonNull Vector3 abs() {
    return new Vector3(Math.abs(x), Math.abs(y), Math.abs(z));
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other instanceof Vector3) {
      final Vector3 v = (Vector3) other;
      if (v.isNaN()) {
        return this.isNaN();
      }
      return (x == v.x) && (y == v.y) && (z == v.z);
    }
    return false;
  }

  @Override
  public int hashCode() {
    if (isNaN()) {
      return 642;
    }
    return 643 * (164 * hash(x) + 3 * hash(y) + hash(z));
  }

  @Override
  public String toString() {
    return "[" + x + ", " + y + ", " + z + "]";
  }

  public @NonNull Vector3 floor() {
    return new Vector3(NumberConversions.floor(x), NumberConversions.floor(y), NumberConversions.floor(z));
  }

  public @NonNull Vector3 snapToBlockCenter() {
    double newX = NumberConversions.floor(x) + 0.5;
    double newY = NumberConversions.floor(y) + 0.5;
    double newZ = NumberConversions.floor(z) + 0.5;
    return new Vector3(newX, newY, newZ);
  }

  public @NonNull IntVector toIntVector() {
    return new IntVector(NumberConversions.floor(x), NumberConversions.floor(y), NumberConversions.floor(z));
  }

  public org.bukkit.util.@NonNull Vector clampVelocity() {
    return min(MAX_VELOCITY).max(MIN_VELOCITY).toBukkitVector();
  }

  public org.bukkit.util.@NonNull Vector toBukkitVector() {
    return new org.bukkit.util.Vector(x, y, z);
  }

  public @NonNull Location toLocation(@NonNull World world) {
    return new Location(world, x, y, z);
  }

  public @NonNull Block toBlock(@NonNull World world) {
    return world.getBlockAt(NumberConversions.floor(x), NumberConversions.floor(y), NumberConversions.floor(z));
  }

  public static double minComponent(@NonNull Vector3 v) {
    return Math.min(v.x, Math.min(v.y, v.z));
  }

  public static double maxComponent(@NonNull Vector3 v) {
    return Math.max(v.x, Math.max(v.y, v.z));
  }

  /**
   * Create a vector at the center of a block position
   */
  public static @NonNull Vector3 center(@NonNull Block b) {
    return new Vector3(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
  }

  private static int hash(double value) {
    return Double.valueOf(value).hashCode();
  }
}
