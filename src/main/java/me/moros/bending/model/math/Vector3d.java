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

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Immutable 3D Vector implementation
 */
public class Vector3d {
  public static final Vector3d ZERO = new Vector3d(0, 0, 0);
  public static final Vector3d ONE = new Vector3d(1, 1, 1);
  public static final Vector3d PLUS_I = new Vector3d(1, 0, 0);
  public static final Vector3d MINUS_I = new Vector3d(-1, 0, 0);
  public static final Vector3d PLUS_J = new Vector3d(0, 1, 0);
  public static final Vector3d MINUS_J = new Vector3d(0, -1, 0);
  public static final Vector3d PLUS_K = new Vector3d(0, 0, 1);
  public static final Vector3d MINUS_K = new Vector3d(0, 0, -1);

  /**
   * Min and max bukkit velocity vectors
   */
  public static final Vector3d MIN_VELOCITY = new Vector3d(-4, -4, -4);
  public static final Vector3d MAX_VELOCITY = new Vector3d(4, 4, 4);

  private final double x;
  private final double y;
  private final double z;

  /**
   * Build a vector from its coordinates.
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   */
  public Vector3d(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * Build a vector from its coordinates.
   * @param v coordinates array
   * @throws IllegalArgumentException if array does not have 3 elements
   * @see #toArray()
   */
  public Vector3d(double[] v) throws IllegalArgumentException {
    if (v.length != 3) {
      throw new IllegalArgumentException("Expected array length " + 3 + " found " + v.length);
    }
    this.x = v[0];
    this.y = v[1];
    this.z = v[2];
  }

  public Vector3d(org.bukkit.util.@NonNull Vector v) {
    this(v.getX(), v.getY(), v.getZ());
  }

  public Vector3d(@NonNull Location l) {
    this(l.getX(), l.getY(), l.getZ());
  }

  public Vector3d(@NonNull Block b) {
    this(b.getX(), b.getY(), b.getZ());
  }

  /**
   * @return the x coordinate
   */
  public double getX() {
    return x;
  }

  /**
   * @return the y coordinate
   */
  public double getY() {
    return y;
  }

  /**
   * @return the z coordinate
   */
  public double getZ() {
    return z;
  }

  /**
   * @return a new vector copy with the given x coordinate
   */
  public @NonNull Vector3d setX(double value) {
    return new Vector3d(value, y, z);
  }

  /**
   * @return a new vector copy with the given y coordinate
   */
  public @NonNull Vector3d setY(double value) {
    return new Vector3d(x, value, z);
  }

  /**
   * @return a new vector copy with the given z coordinate
   */
  public @NonNull Vector3d setZ(double value) {
    return new Vector3d(x, y, value);
  }

  /**
   * Get the vector coordinates as a dimension 3 array.
   * @return vector coordinates
   * @see #Vector3d(double[])
   */
  public double[] toArray() {
    return new double[]{x, y, z};
  }

  /**
   * @return Euclidean norm for the vector
   */
  public double length() {
    return Math.sqrt(x * x + y * y + z * z);
  }

  /**
   * @return square of the Euclidean norm for the vector
   */
  public double lengthSq() {
    return x * x + y * y + z * z;
  }

  /**
   * Add a vector to the instance.
   * @param v vector to add
   * @return a new vector
   */
  public @NonNull Vector3d add(@NonNull Vector3d v) {
    return new Vector3d(x + v.x, y + v.y, z + v.z);
  }

  /**
   * Subtract a vector from the instance.
   * @param v vector to subtract
   * @return a new vector
   */
  public @NonNull Vector3d subtract(@NonNull Vector3d v) {
    return new Vector3d(x - v.x, y - v.y, z - v.z);
  }

  /**
   * @return {@link #normalize(Vector3d)} with {@link #PLUS_I} as default.
   */
  public @NonNull Vector3d normalize() {
    return normalize(Vector3d.PLUS_I);
  }

  /**
   * Get a normalized vector aligned with the instance.
   * @param def the default vector to return if norm is zero
   * @return a new normalized vector
   */
  public @NonNull Vector3d normalize(@NonNull Vector3d def) {
    double s = length();
    if (s == 0) {
      return def;
    }
    return multiply(1 / s);
  }

  /**
   * Get the opposite of the instance.
   * @return a new vector which is opposite to the instance
   */
  public @NonNull Vector3d negate() {
    return new Vector3d(-x, -y, -z);
  }

  /**
   * Multiply the instance by a scalar.
   * @param a scalar
   * @return a new vector
   */
  public @NonNull Vector3d multiply(double a) {
    return new Vector3d(a * x, a * y, a * z);
  }

  public @NonNull Vector3d multiply(@NonNull Vector3d v) {
    return new Vector3d(x * v.x, y * v.y, z * v.z);
  }

  /**
   * @return true if any coordinate of this point is NaN, false otherwise
   */
  public boolean isNaN() {
    return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
  }

  /**
   * @return true if any coordinate of this vector is infinite and none are NaN, false otherwise
   */
  public boolean isInfinite() {
    return !isNaN() && (Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z));
  }

  public double dot(@NonNull Vector3d v) {
    return x * v.x + y * v.y + z * v.z;
  }

  public @NonNull Vector3d cross(@NonNull Vector3d v) {
    double newX = y * v.z - v.y * z;
    double newY = z * v.x - v.z * x;
    double newZ = x * v.y - v.x * y;
    return new Vector3d(newX, newY, newZ);
  }

  public double distance(@NonNull Vector3d v) {
    double dx = v.x - x;
    double dy = v.y - y;
    double dz = v.z - z;
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  public double distanceSq(@NonNull Vector3d v) {
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
  public double angle(@NonNull Vector3d v) {
    double normProduct = length() * v.length();
    if (normProduct == 0) {
      return 0;
    }
    double dot = Math.min(Math.max(dot(v) / normProduct, -1), 1);
    return Math.acos(dot);
  }

  public @NonNull Vector3d min(@NonNull Vector3d v) {
    return new Vector3d(Math.min(x, v.x), Math.min(y, v.y), Math.min(z, v.z));
  }

  public @NonNull Vector3d max(@NonNull Vector3d v) {
    return new Vector3d(Math.max(x, v.x), Math.max(y, v.y), Math.max(z, v.z));
  }

  public @NonNull Vector3d abs() {
    return new Vector3d(Math.abs(x), Math.abs(y), Math.abs(z));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Vector3d v) {
      return v.isNaN() ? this.isNaN() : (x == v.x) && (y == v.y) && (z == v.z);
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

  public @NonNull Vector3d floor() {
    return new Vector3d(FastMath.floor(x), FastMath.floor(y), FastMath.floor(z));
  }

  public @NonNull Vector3d snapToBlockCenter() {
    double newX = FastMath.floor(x) + 0.5;
    double newY = FastMath.floor(y) + 0.5;
    double newZ = FastMath.floor(z) + 0.5;
    return new Vector3d(newX, newY, newZ);
  }

  public @NonNull Vector3i toVector3i() {
    return new Vector3i(FastMath.floor(x), FastMath.floor(y), FastMath.floor(z));
  }

  public org.bukkit.util.@NonNull Vector clampVelocity() {
    double clampedX = Math.min(MAX_VELOCITY.x, Math.max(MIN_VELOCITY.x, x));
    double clampedY = Math.min(MAX_VELOCITY.y, Math.max(MIN_VELOCITY.y, y));
    double clampedZ = Math.min(MAX_VELOCITY.z, Math.max(MIN_VELOCITY.z, z));
    return new org.bukkit.util.Vector(clampedX, clampedY, clampedZ);
  }

  public org.bukkit.util.@NonNull Vector toBukkitVector() {
    return new org.bukkit.util.Vector(x, y, z);
  }

  public @NonNull Location toLocation(@NonNull World world) {
    return new Location(world, x, y, z);
  }

  public @NonNull Block toBlock(@NonNull World world) {
    return world.getBlockAt(FastMath.floor(x), FastMath.floor(y), FastMath.floor(z));
  }

  public static double minComponent(@NonNull Vector3d v) {
    return Math.min(v.x, Math.min(v.y, v.z));
  }

  public static double maxComponent(@NonNull Vector3d v) {
    return Math.max(v.x, Math.max(v.y, v.z));
  }

  /**
   * Create a vector at the center of a block position.
   */
  public static @NonNull Vector3d center(@NonNull Block b) {
    return new Vector3d(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
  }

  private static int hash(double value) {
    return Double.valueOf(value).hashCode();
  }
}
