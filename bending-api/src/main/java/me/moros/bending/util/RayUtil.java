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

package me.moros.bending.util;

import java.util.ArrayList;
import java.util.Collection;

import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.user.User;
import me.moros.math.Vector3d;

/**
 * Utility class for generating arrays of rays.
 */
public final class RayUtil {
  private static final double ANGLE_STEP = Math.toRadians(10);
  private static final double ANGLE = Math.toRadians(30);
  private static final double FALL_MIN_ANGLE = Math.toRadians(60);
  private static final double FALL_MAX_ANGLE = Math.toRadians(105);

  private RayUtil() {
  }

  /**
   * Create a burst of rays in the shape of a cone. The cone's tip is centered at the user.
   * @param user the user creating the cone burst
   * @param range the length of each ray
   * @return the bursting rays
   * @see #createBurst(User, double, double, double)
   */
  public static Collection<Ray> cone(User user, double range) {
    return createBurst(user, range, ANGLE_STEP, ANGLE);
  }

  /**
   * Create a burst of rays in the shape of a sphere. The sphere is centered around the user.
   * @param user the user creating the sphere burst
   * @param range the length of each ray
   * @return the bursting rays
   * @see #createBurst(User, double, double, double)
   */
  public static Collection<Ray> sphere(User user, double range) {
    return createBurst(user, range, ANGLE_STEP, 0);
  }

  /**
   * Create a burst of rays in a hybrid shape between a cone and a sphere. The burst is centered around the user.
   * @param user the user creating the fall burst
   * @param range the length of each ray
   * @return the bursting rays
   * @see #createBurst(User, double, double, double)
   */
  public static Collection<Ray> fall(User user, double range) {
    return createBurst(user, range, ANGLE_STEP, -1);
  }

  /**
   * Create a burst of rays with the specified parameters.
   * <p>Note: An angle of 0 makes the burst spherical while a negative angle makes the burst a hybrid between a sphere and a cone.
   * @param user the user creating the burst
   * @param range the length of each ray
   * @param angleStep the delta angle between rays in radians
   * @param angle the angle of the burst in radians
   * @return the bursting rays
   */
  // Negative angle for fall burst
  public static Collection<Ray> createBurst(User user, double range, double angleStep, double angle) {
    Vector3d center = EntityUtil.entityCenter(user.entity());
    Vector3d userDIr = user.direction();
    Collection<Ray> rays = new ArrayList<>();
    double epsilon = 0.001; // Needed for accuracy
    for (double theta = 0; theta < Math.PI - epsilon; theta += angleStep) {
      double z = Math.cos(theta);
      double sinTheta = Math.sin(theta);
      for (double phi = 0; phi < 2 * Math.PI - epsilon; phi += angleStep) {
        double x = Math.cos(phi) * sinTheta;
        double y = Math.sin(phi) * sinTheta;
        Vector3d direction = Vector3d.of(x, y, z);
        if (angle > 0 && direction.angle(userDIr) > angle) {
          continue;
        }
        if (angle < 0) {
          double vectorAngle = direction.angle(Vector3d.PLUS_J);
          if (vectorAngle < FALL_MIN_ANGLE || vectorAngle > FALL_MAX_ANGLE) {
            continue;
          }
        }
        rays.add(new Ray(center, direction.multiply(range)));
      }
    }
    return rays;
  }
}
