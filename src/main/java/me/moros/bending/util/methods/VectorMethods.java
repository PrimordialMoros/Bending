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

package me.moros.bending.util.methods;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Rotation;
import me.moros.bending.model.math.Vector3;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class with useful {@link Vector3} related methods.
 */
public final class VectorMethods {
  private VectorMethods() {
  }

  /**
   * Create an arc by combining {@link #rotate(Vector3, Rotation, int)} and {@link #rotateInverse(Vector3, Rotation, int)}.
   * Amount of rays will be rounded up to the nearest odd number. Minimum value is 3.
   * @param start the starting point
   * @param axis the axis around which to rotate
   * @param angle the rotation angle
   * @param rays the amount of vectors to return, must be an odd number, minimum 3
   * @return a list comprising of all the directions for this arc
   * @see #rotateInverse(Vector3, Rotation, int)
   */
  public static @NonNull Collection<@NonNull Vector3> createArc(@NonNull Vector3 start, @NonNull Vector3 axis, double angle, int rays) {
    Rotation rotation = new Rotation(axis, angle);
    rays = Math.max(3, rays);
    if (rays % 2 == 0) {
      rays++;
    }
    int half = (rays - 1) / 2;
    Collection<Vector3> arc = new ArrayList<>(rays);
    arc.add(start);
    arc.addAll(rotate(start, rotation, half));
    arc.addAll(rotateInverse(start, rotation, half));
    return arc;
  }

  public static @NonNull Collection<@NonNull Vector3> circle(@NonNull Vector3 start, @NonNull Vector3 axis, int times) {
    double angle = 2 * Math.PI / times;
    return rotate(start, axis, angle, times);
  }

  /**
   * Repeat a rotation on a specific vector.
   * @param start the starting point
   * @param axis the axis around which to rotate
   * @param angle the rotation angle
   * @param times the amount of times to repeat the rotation
   * @return a list comprising of all the directions for this arc
   * @see #rotateInverse(Vector3, Rotation, int)
   */
  public static @NonNull Collection<@NonNull Vector3> rotate(@NonNull Vector3 start, @NonNull Vector3 axis, double angle, int times) {
    return rotate(start, new Rotation(axis, angle), times);
  }

  private static @NonNull Collection<@NonNull Vector3> rotate(@NonNull Vector3 start, @NonNull Rotation rotation, int times) {
    Collection<Vector3> arc = new ArrayList<>();
    double[] vector = start.toArray();
    for (int i = 0; i < times; i++) {
      rotation.applyTo(vector, vector);
      arc.add(new Vector3(vector));
    }
    return arc;
  }

  /**
   * Inversely repeat a rotation on a specific vector.
   * @see #rotate(Vector3, Rotation, int)
   */
  public static @NonNull Collection<@NonNull Vector3> rotateInverse(@NonNull Vector3 start, @NonNull Vector3 axis, double angle, int times) {
    return rotateInverse(start, new Rotation(axis, angle), times);
  }

  private static @NonNull Collection<@NonNull Vector3> rotateInverse(@NonNull Vector3 start, @NonNull Rotation rotation, int times) {
    Collection<Vector3> arc = new ArrayList<>();
    double[] vector = start.toArray();
    for (int i = 0; i < times; i++) {
      rotation.applyInverseTo(vector, vector);
      arc.add(new Vector3(vector));
    }
    return arc;
  }

  /**
   * Get an orthogonal vector.
   */
  public static @NonNull Vector3 orthogonal(@NonNull Vector3 axis, double radians, double length) {
    double[] arr = {axis.y, -axis.x, 0};
    Rotation rotation = new Rotation(axis, radians);
    return rotation.applyTo(new Vector3(arr).normalize().multiply(length));
  }

  /**
   * Rotate a vector around the X axis.
   * @param v the vector to rotate
   * @param cos the rotation's cosine
   * @param sin the rotation's sine
   * @return the resulting vector
   * @see #rotateAroundAxisY(Vector3, double, double)
   * @see #rotateAroundAxisZ(Vector3, double, double)
   */
  public static @NonNull Vector3 rotateAroundAxisX(@NonNull Vector3 v, double cos, double sin) {
    return new Vector3(v.x, v.y * cos - v.z * sin, v.y * sin + v.z * cos);
  }

  /**
   * Rotate a vector around the Y axis.
   * @param v the vector to rotate
   * @param cos the rotation's cosine
   * @param sin the rotation's sine
   * @return the resulting vector
   * @see #rotateAroundAxisX(Vector3, double, double)
   * @see #rotateAroundAxisZ(Vector3, double, double)
   */
  public static @NonNull Vector3 rotateAroundAxisY(@NonNull Vector3 v, double cos, double sin) {
    return new Vector3(v.x * cos + v.z * sin, v.y, v.x * -sin + v.z * cos);
  }

  /**
   * Rotate a vector around the Z axis.
   * @param v the vector to rotate
   * @param cos the rotation's cosine
   * @param sin the rotation's sine
   * @return the resulting vector
   * @see #rotateAroundAxisX(Vector3, double, double)
   * @see #rotateAroundAxisY(Vector3, double, double)
   */
  public static @NonNull Vector3 rotateAroundAxisZ(@NonNull Vector3 v, double cos, double sin) {
    return new Vector3(v.x * cos - v.y * sin, v.x * sin + v.y * cos, v.z);
  }

  /**
   * Decompose diagonal vectors into their cardinal components so they can be checked individually.
   * This is helpful for resolving collisions when moving blocks diagonally and need to consider all block faces.
   * @param origin the point of origin
   * @param direction the direction to check
   * @return a collection of normalized vectors corresponding to cardinal block faces
   */
  public static @NonNull Collection<IntVector> decomposeDiagonals(@NonNull Vector3 origin, @NonNull Vector3 direction) {
    double[] o = origin.toArray();
    double[] d = direction.toArray();
    Collection<IntVector> possibleCollisions = new ArrayList<>(3);
    for (int i = 0; i < 3; i++) {
      int a = NumberConversions.floor(o[i] + d[i]);
      int b = NumberConversions.floor(o[i]);
      int delta = Math.min(1, Math.max(-1, a - b));
      if (delta != 0) {
        int[] v = new int[]{0, 0, 0};
        v[i] = delta;
        possibleCollisions.add(new IntVector(v));
      }
    }
    if (possibleCollisions.isEmpty()) {
      return List.of(IntVector.ZERO);
    }
    return possibleCollisions;
  }

  public static @NonNull Vector3 gaussianOffset(Vector3 target, double offset) {
    return gaussianOffset(target, offset, offset, offset);
  }

  public static @NonNull Vector3 gaussianOffset(Vector3 target, double offsetX, double offsetY, double offsetZ) {
    ThreadLocalRandom r = ThreadLocalRandom.current();
    double[] v = {r.nextGaussian() * offsetX, r.nextGaussian() * offsetY, r.nextGaussian() * offsetZ};
    return target.add(new Vector3(v));
  }
}
