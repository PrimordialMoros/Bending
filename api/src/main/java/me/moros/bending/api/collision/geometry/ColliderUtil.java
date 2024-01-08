/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.collision.geometry;

import java.util.function.BiPredicate;

import me.moros.bending.api.collision.geometry.Collider.Type;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;

import static java.lang.Math.abs;

@SuppressWarnings({"rawtypes", "unchecked"})
final class ColliderUtil {
  private ColliderUtil() {
  }

  private static final Resolver[][] RESOLVERS;

  static {
    RESOLVERS = new Resolver[5][];
    for (int i = 0; i < RESOLVERS.length; i++) {
      RESOLVERS[i] = new Resolver[5];
    }
    // Identity
    addMapping(Type.SPHERE, Type.SPHERE, ColliderUtil::sphereIntersection);
    addMapping(Type.AABB, Type.AABB, ColliderUtil::aabbIntersection);
    addMapping(Type.OBB, Type.OBB, ColliderUtil::obbIntersection);
    addMapping(Type.RAY, Type.RAY, ColliderUtil::rayIntersection);
    addMapping(Type.DISK, Type.DISK, ColliderUtil::diskIntersection);

    // Other
    addMapping(Type.SPHERE, Type.RAY, ColliderUtil::sphereIntersectsRay);

    addMapping(Type.AABB, Type.SPHERE, ColliderUtil::aabbIntersectsSphere);
    addMapping(Type.AABB, Type.RAY, ColliderUtil::aabbIntersectsRay);

    addMapping(Type.OBB, Type.SPHERE, ColliderUtil::obbIntersectsSphere);
    addMapping(Type.OBB, Type.AABB, ColliderUtil::obbIntersectsAabb);
    addMapping(Type.OBB, Type.RAY, ColliderUtil::obbIntersectsRay);

    addMapping(Type.DISK, Type.SPHERE, ColliderUtil::diskIntersectsSphere);
    addMapping(Type.DISK, Type.AABB, ColliderUtil::diskIntersectsAabb);
    addMapping(Type.DISK, Type.OBB, ColliderUtil::diskIntersectsObb);
    addMapping(Type.DISK, Type.RAY, ColliderUtil::diskIntersectsRay);
  }

  private static <C0 extends Collider, C1 extends Collider> void addMapping(Type first, Type second, Resolver<C0, C1> resolver) {
    final int firstId = first.ordinal();
    final int secondId = second.ordinal();
    RESOLVERS[firstId][secondId] = resolver;
    if (firstId != secondId) {
      RESOLVERS[secondId][firstId] = resolver.inverse();
    }
  }

  @FunctionalInterface
  private interface Resolver<C0 extends Collider, C1 extends Collider> extends BiPredicate<C0, C1> {
    default Resolver<C1, C0> inverse() {
      return (first, second) -> test(second, first);
    }
  }

  static boolean intersects(Collider first, Collider second) {
    return RESOLVERS[first.type().ordinal()][second.type().ordinal()].test(first, second);
  }

  private static boolean aabbIntersectsSphere(AABB aabb, Sphere sphere) {
    Vector3d min = aabb.min();
    Vector3d max = aabb.max();
    // Get the point closest to sphere center on the aabb.
    double x = FastMath.clamp(sphere.position().x(), min.x(), max.x());
    double y = FastMath.clamp(sphere.position().y(), min.y(), max.y());
    double z = FastMath.clamp(sphere.position().z(), min.z(), max.z());
    // Check if that point is inside the sphere.
    return sphere.contains(Vector3d.of(x, y, z));
  }

  private static boolean sphereIntersectsRay(Sphere sphere, Ray ray) {
    Vector3d m = ray.position().subtract(sphere.position());
    double b = ray.direction().dot(m);
    return (m.lengthSq() - b * b / ray.direction().lengthSq()) <= sphere.radius() * sphere.radius();
  }

  private static boolean aabbIntersectsRay(AABB aabb, Ray ray) {
    Vector3d t0 = aabb.min().subtract(ray.position()).multiply(ray.inv());
    Vector3d t1 = aabb.max().subtract(ray.position()).multiply(ray.inv());
    return t0.min(t1).maxComponent() <= t0.max(t1).minComponent();
  }

  private static boolean obbIntersectsSphere(OBB obb, Sphere sphere) {
    Vector3d v = sphere.position().subtract(obb.closestPosition(sphere.position()));
    return v.dot(v) <= sphere.radius() * sphere.radius();
  }

  private static boolean obbIntersectsAabb(OBB obb, AABB aabb) {
    return obbIntersection(obb, OBB.of(aabb));
  }

  private static boolean obbIntersectsRay(OBB obb, Ray ray) {
    Ray localRay = Ray.of(obb.localSpace(ray.position()), obb.localSpace(ray.direction()));
    AABB localAABB = AABB.of(obb.extents().negate(), obb.extents()).at(obb.position());
    return aabbIntersectsRay(localAABB, localRay);
  }

  private static boolean diskIntersectsSphere(Disk disk, Sphere sphere) {
    return sphereIntersection(disk.sphere(), sphere) && obbIntersectsSphere(disk.obb(), sphere);
  }

  private static boolean diskIntersectsAabb(Disk disk, AABB aabb) {
    return aabbIntersectsSphere(aabb, disk.sphere()) && obbIntersection(disk.obb(), OBB.of(aabb));
  }

  private static boolean diskIntersectsObb(Disk disk, OBB obb) {
    return obbIntersectsSphere(obb, disk.sphere()) && obbIntersection(disk.obb(), obb);
  }

  private static boolean diskIntersectsRay(Disk disk, Ray ray) {
    return sphereIntersectsRay(disk.sphere(), ray) && obbIntersectsRay(disk.obb(), ray);
  }

  private static boolean sphereIntersection(Sphere first, Sphere other) {
    // Spheres will be colliding if their distance apart is less than the sum of the radii.
    double sum = first.radius() + other.radius();
    return other.position().distanceSq(first.position()) <= sum * sum;
  }

  private static boolean aabbIntersection(AABB first, AABB other) {
    return (first.max().x() > other.min().x() && first.min().x() < other.max().x() &&
      first.max().y() > other.min().y() && first.min().y() < other.max().y() &&
      first.max().z() > other.min().z() && first.min().z() < other.max().z());
  }

  private static boolean obbIntersection(OBB first, OBB other) {
    if (!aabbIntersection(first.outer(), other.outer())) {
      return false;
    }
    final Vector3d pos = other.position().subtract(first.position());
    for (int i = 0; i < 3; i++) {
      if (getSeparatingPlane(first, pos, first.axis(i), other) || getSeparatingPlane(first, pos, other.axis(i), other)) {
        return false;
      }
    }
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (getSeparatingPlane(first, pos, first.axis(i).cross(other.axis(j)), other)) {
          return false;
        }
      }
    }
    return true;
  }

  // check if there's a separating plane in between the selected axes
  private static boolean getSeparatingPlane(OBB first, Vector3d pos, Vector3d plane, OBB other) {
    final double dot = abs(pos.dot(plane));
    final double x1 = abs((first.axis(0).multiply(first.extents().x())).dot(plane));
    final double y1 = abs((first.axis(1).multiply(first.extents().y())).dot(plane));
    final double z1 = abs((first.axis(2).multiply(first.extents().z())).dot(plane));
    final double x2 = abs((other.axis(0).multiply(other.extents().x())).dot(plane));
    final double y2 = abs((other.axis(1).multiply(other.extents().y())).dot(plane));
    final double z2 = abs((other.axis(2).multiply(other.extents().z())).dot(plane));
    return dot > x1 + y1 + z1 + x2 + y2 + z2;
  }

  private static boolean rayIntersection(Ray first, Ray other) {
    Vector3d cross = first.direction().cross(other.direction());
    if (cross.lengthSq() < Collider.EPSILON) {
      return first.contains(other.position()) || other.contains(first.position());
    }
    double planarFactor = other.position().subtract(first.position()).dot(cross);
    return abs(planarFactor) < Collider.EPSILON;
  }

  private static boolean diskIntersection(Disk first, Disk other) {
    return sphereIntersection(first.sphere(), other.sphere()) && obbIntersection(first.obb(), other.obb());
  }
}
