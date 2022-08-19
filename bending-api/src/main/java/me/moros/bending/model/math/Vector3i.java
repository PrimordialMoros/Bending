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

import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * Immutable 3D Vector implementation with integer coordinates.
 */
public final class Vector3i {
  public static final Vector3i ZERO = new Vector3i(0, 0, 0);

  private final int x;
  private final int y;
  private final int z;

  /**
   * Build a vector from its coordinates.
   * @param x the x coordinate
   * @param y the y coordinate
   * @param z the z coordinate
   */
  public Vector3i(int x, int y, int z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public Vector3i(Block b) {
    this(b.getX(), b.getY(), b.getZ());
  }

  /**
   * Build a vector from its coordinates.
   * @param v coordinates array
   * @throws IllegalArgumentException if array does not have 3 elements
   * @see #toArray()
   */
  public Vector3i(int[] v) throws IllegalArgumentException {
    if (v.length != 3) {
      throw new IllegalArgumentException();
    }
    this.x = v[0];
    this.y = v[1];
    this.z = v[2];
  }

  /**
   * Get the x coordinate.
   * @return the x coordinate
   */
  public int x() {
    return x;
  }

  /**
   * Get the y coordinate.
   * @return the y coordinate
   */
  public int y() {
    return y;
  }

  /**
   * Get the z coordinate.
   * @return the z coordinate
   */
  public int z() {
    return z;
  }

  /**
   * Compute a vector from this instance with x coordinate set to value.
   * @param value the value to set to x coordinate
   * @return a new vector with the given x coordinate
   */
  public Vector3i withX(int value) {
    return new Vector3i(value, y, z);
  }

  /**
   * Compute a vector from this instance with y coordinate set to value.
   * @param value the value to set to y coordinate
   * @return a new vector with the given y coordinate
   */
  public Vector3i withY(int value) {
    return new Vector3i(x, value, z);
  }

  /**
   * Compute a vector from this instance with z coordinate set to value.
   * @param value the value to set to z coordinate
   * @return a new vector with the given z coordinate
   */
  public Vector3i withZ(int value) {
    return new Vector3i(x, y, value);
  }

  /**
   * Get the vector coordinates as a dimension 3 array.
   * @return vector coordinates
   * @see #Vector3i(int[])
   */
  public int[] toArray() {
    return new int[]{x, y, z};
  }

  /**
   * Get the norm for this instance.
   * @return Euclidean norm for the vector
   */
  public double length() {
    return Math.sqrt(lengthSq());
  }

  /**
   * Get the square of the norm for this instance.
   * @return square of the Euclidean norm for the vector
   */
  public int lengthSq() {
    return x * x + y * y + z * z;
  }

  /**
   * Add a vector to the instance.
   * @param v vector to add
   * @return a new vector
   */
  public Vector3i add(Vector3i v) {
    return new Vector3i(x + v.x, y + v.y, z + v.z);
  }

  /**
   * Add values to the instance.
   * @param dx the amount to add for the x coordinate
   * @param dy the amount to add for the y coordinate
   * @param dz the amount to add for the z coordinate
   * @return a new vector
   */
  public Vector3i add(int dx, int dy, int dz) {
    return new Vector3i(x + dx, y + dy, z + dz);
  }

  /**
   * Subtract a vector from the instance.
   * @param v vector to subtract
   * @return a new vector
   */
  public Vector3i subtract(Vector3i v) {
    return new Vector3i(x - v.x, y - v.y, z - v.z);
  }

  /**
   * Subtract values from the instance.
   * @param dx the amount to subtract for the x coordinate
   * @param dy the amount to subtract for the y coordinate
   * @param dz the amount to subtract for the z coordinate
   * @return a new vector
   */
  public Vector3i subtract(int dx, int dy, int dz) {
    return new Vector3i(x - dx, y - dy, z - dz);
  }

  /**
   * Get the opposite of the instance.
   * @return a new vector which is opposite to the instance
   */
  public Vector3i negate() {
    return new Vector3i(-x, -y, -z);
  }

  /**
   * Multiply the instance by a scalar.
   * @param a scalar
   * @return a new vector
   */
  public Vector3i multiply(int a) {
    return new Vector3i(a * x, a * y, a * z);
  }

  /**
   * Multiply this instance by the components of the given vector.
   * @param v the vector to multiply by
   * @return a new vector
   */
  public Vector3i multiply(Vector3i v) {
    return new Vector3i(x * v.x, y * v.y, z * v.z);
  }

  /**
   * Compute the dot-product of this instance with the given vector.
   * @param v the other vector
   * @return the dot product
   */
  public int dot(Vector3i v) {
    return x * v.x + y * v.y + z * v.z;
  }

  /**
   * Compute the cross-product of this instance with the given vector.
   * @param v the other vector
   * @return the cross product this.v
   */
  public Vector3i cross(Vector3i v) {
    int newX = y * v.z - v.y * z;
    int newY = z * v.x - v.z * x;
    int newZ = x * v.y - v.x * y;
    return new Vector3i(newX, newY, newZ);
  }

  /**
   * Compute the distance between the instance and another vector.
   * @param v second vector
   * @return the distance between the instance and p
   */
  public double distance(Vector3i v) {
    return Math.sqrt(distanceSq(v));
  }

  /**
   * Compute the square of the distance between the instance and another vector.
   * @param v second vector
   * @return the square of the distance between the instance and p
   */
  public int distanceSq(Vector3i v) {
    int dx = v.x - x;
    int dy = v.y - y;
    int dz = v.z - z;
    return dx * dx + dy * dy + dz * dz;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Vector3i v) {
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

  /**
   * Create a double vector from this instance.
   * @return a new vector
   */
  public Vector3d toVector3d() {
    return new Vector3d(x, y, z);
  }

  /**
   * Get the block at the point of this vector for the given world
   * @param world the world to check
   * @return the block
   */
  public Block toBlock(World world) {
    return world.getBlockAt(x, y, z);
  }
}
