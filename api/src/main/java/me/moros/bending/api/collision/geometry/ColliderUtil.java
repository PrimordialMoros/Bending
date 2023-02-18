/*
 * Copyright 2020-2023 Moros
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

import me.moros.math.FastMath;
import me.moros.math.Vector3d;

import static java.lang.Math.abs;

final class ColliderUtil {
  private ColliderUtil() {
  }

  /*private static boolean aabbIntersects(AABB aabb, Collider second) {
    return switch (second) {
      case AABB other -> _intersects(aabb, other);
      case Sphere sphere -> _intersects(aabb, sphere);
      case OBB obb -> _intersects((OBBImpl) OBB.of(aabb), (OBBImpl) obb);
      case Disk disk -> _intersects(aabb, disk.sphere()) && _intersects((OBBImpl) disk.obb(), (OBBImpl) OBB.of(aabb));
      case Ray ray -> _intersects(ray, aabb);
    };
  }

  private static boolean sphereIntersects(Sphere sphere, Collider second) {
    return switch (second) {
      case AABB aabb -> _intersects(aabb, sphere);
      case Sphere other -> _intersects(sphere, other);
      case OBB obb -> _intersects(obb, sphere);
      case Disk disk -> _intersects(sphere, disk.sphere()) && _intersects(disk.obb(), sphere);
      case Ray ray -> _intersects(ray, sphere);
    };
  }

  private static boolean obbIntersects(OBB obb, Collider second) {
    var obbFirst = (OBBImpl) obb;
    return switch (second) {
      case AABB aabb -> _intersects(obbFirst, (OBBImpl) OBB.of(aabb));
      case Sphere sphere -> _intersects(obb, sphere);
      case OBB other -> _intersects(obbFirst, (OBBImpl) other);
      case Disk disk -> _intersects(obb, disk.sphere()) && _intersects(obbFirst, (OBBImpl) disk.obb());
      case Ray ray -> _intersects(obbFirst, ray);
    };
  }

  private static boolean diskIntersects(Disk disk, Collider second) {
    var obbFirst = (OBBImpl) disk.obb();
    return switch (second) {
      case AABB aabb -> _intersects(aabb, disk.sphere()) && _intersects(obbFirst, (OBBImpl) OBB.of(aabb));
      case Sphere sphere -> _intersects(disk.sphere(), sphere) && _intersects(disk.obb(), sphere);
      case OBB obb -> _intersects(obb, disk.sphere()) && _intersects(obbFirst, (OBBImpl) obb);
      case Disk other -> _intersects(disk.sphere(), other.sphere()) && _intersects(obbFirst, (OBBImpl) other.obb());
      case Ray ray -> _intersects(ray, disk.sphere()) && _intersects(obbFirst, ray);
    };
  }

  private static boolean rayIntersects(Ray ray, Collider second) {
    return switch (second) {
      case AABB aabb -> _intersects(ray, aabb);
      case Sphere sphere -> _intersects(ray, sphere);
      case OBB obb -> _intersects((OBBImpl) obb, ray);
      case Disk disk -> _intersects(ray, disk.sphere()) && _intersects((OBBImpl) disk.obb(), ray);
      case Ray other -> _intersects(ray, other);
    };
  }

  static boolean intersects(Collider first, Collider second) {
    if (first.equals(AABB.dummy()) || second.equals(AABB.dummy())) {
      return false;
    }
    return switch (first) {
      case AABB aabb -> aabbIntersects(aabb, second);
      case Sphere sphere -> sphereIntersects(sphere, second);
      case OBB obb -> obbIntersects(obb, second);
      case Disk disk -> diskIntersects(disk, second);
      case Ray ray -> rayIntersects(ray, second);
    };
  }*/

  static boolean intersects(Collider first, Collider second) {
    if (first.equals(AABB.dummy()) || second.equals(AABB.dummy())) {
      return false;
    } else if (first instanceof Sphere sphere1) {
      if (second instanceof Sphere sphere2) {
        return _intersects(sphere1, sphere2);
      } else if (second instanceof AABB aabb) {
        return _intersects(aabb, sphere1);
      } else if (second instanceof OBB obb) {
        return _intersects(obb, sphere1);
      } else if (second instanceof Disk disk) {
        return _intersects(disk.sphere(), sphere1) && _intersects(disk.obb(), sphere1);
      } else if (second instanceof Ray ray) {
        return _intersects(ray, sphere1);
      }
    } else if (first instanceof AABB aabb1) {
      if (second instanceof AABB aabb2) {
        return _intersects(aabb1, aabb2);
      } else if (second instanceof OBB obb) {
        return _intersects((OBBImpl)obb, (OBBImpl) OBB.of(aabb1));
      } else if (second instanceof Sphere sphere) {
        return _intersects(aabb1, sphere);
      } else if (second instanceof Disk disk) {
        return _intersects(aabb1, disk.sphere()) && _intersects((OBBImpl) disk.obb(), (OBBImpl) OBB.of(aabb1));
      } else if (second instanceof Ray ray) {
        return _intersects(ray, aabb1);
      }
    } else if (first instanceof OBB obb1) {
      if (second instanceof OBB obb2) {
        return _intersects((OBBImpl) obb1, (OBBImpl) obb2);
      } else if (second instanceof AABB aabb) {
        return _intersects((OBBImpl) obb1, (OBBImpl) OBB.of(aabb));
      } else if (second instanceof Sphere sphere) {
        return _intersects(obb1, sphere);
      } else if (second instanceof Disk disk) {
        return _intersects(obb1, disk.sphere()) && _intersects((OBBImpl) obb1, (OBBImpl) disk.obb());
      } else if (second instanceof Ray ray) {
        return _intersects((OBBImpl) obb1, ray);
      }
    } else if (first instanceof Disk disk1) {
      if (second instanceof Disk disk2) {
        return _intersects(disk1.sphere(), disk2.sphere()) && _intersects((OBBImpl) disk1.obb(), (OBBImpl) disk2.obb());
      } else if (second instanceof AABB aabb) {
        return _intersects(aabb, disk1.sphere()) && _intersects((OBBImpl) disk1.obb(), (OBBImpl) OBB.of(aabb));
      } else if (second instanceof OBB obb) {
        return _intersects(obb, disk1.sphere()) && _intersects((OBBImpl) obb, (OBBImpl) disk1.obb());
      } else if (second instanceof Sphere sphere) {
        return _intersects(disk1.sphere(), sphere) && _intersects(disk1.obb(), sphere);
      } else if (second instanceof Ray ray) {
        return _intersects(ray, disk1.sphere()) && _intersects((OBBImpl) disk1.obb(), ray);
      }
    } else if (first instanceof Ray ray1) {
      if (second instanceof Ray ray2) {
        return _intersects(ray1, ray2);
      } else if (second instanceof AABB aabb) {
        return _intersects(ray1, aabb);
      } else if (second instanceof OBB obb) {
        return _intersects((OBBImpl) obb, ray1);
      } else if (second instanceof Sphere sphere) {
        return _intersects(ray1, sphere);
      } else if (second instanceof Disk disk) {
        return _intersects(ray1, disk.sphere()) && _intersects((OBBImpl) disk.obb(), ray1);
      }
    }
    return false;
  }

  private static boolean _intersects(AABB aabb, Sphere sphere) {
    Vector3d min = aabb.min();
    Vector3d max = aabb.max();
    // Get the point closest to sphere center on the aabb.
    double x = FastMath.clamp(sphere.position().x(), min.x(), max.x());
    double y = FastMath.clamp(sphere.position().y(), min.y(), max.y());
    double z = FastMath.clamp(sphere.position().z(), min.z(), max.z());
    // Check if that point is inside the sphere.
    return sphere.contains(Vector3d.of(x, y, z));
  }

  private static boolean _intersects(OBB obb, Sphere sphere) {
    Vector3d v = sphere.position().subtract(obb.closestPosition(sphere.position()));
    return v.dot(v) <= sphere.radius() * sphere.radius();
  }

  private static boolean _intersects(OBBImpl obb, Ray ray) {
    Ray localRay = Ray.of(OBBImpl.localSpace(obb.axes(), ray.position()), OBBImpl.localSpace(obb.axes(), ray.direction()));
    AABB localAABB = AABB.of(obb.extents().negate(), obb.extents()).at(obb.position());
    return _intersects(localRay, localAABB);
  }

  private static boolean _intersects(Ray ray, Sphere sphere) {
    Vector3d m = ray.position().subtract(sphere.position());
    double b = m.dot(ray.direction());
    return b * b - (m.dot(m) - sphere.radius() * sphere.radius()) >= 0;
  }

  private static boolean _intersects(Ray ray, AABB aabb) {
    Vector3d t0 = aabb.min().subtract(ray.position()).multiply(ray.inv());
    Vector3d t1 = aabb.max().subtract(ray.position()).multiply(ray.inv());
    return t0.min(t1).maxComponent() <= t0.max(t1).minComponent();
  }

  private static boolean _intersects(Sphere first, Sphere other) {
    // Spheres will be colliding if their distance apart is less than the sum of the radii.
    double sum = first.radius() + other.radius();
    return other.position().distanceSq(first.position()) <= sum * sum;
  }

  private static boolean _intersects(AABB first, AABB other) {
    return (first.max().x() > other.min().x() && first.min().x() < other.max().x() &&
      first.max().y() > other.min().y() && first.min().y() < other.max().y() &&
      first.max().z() > other.min().z() && first.min().z() < other.max().z());
  }

  private static boolean _intersects(OBBImpl first, OBBImpl other) {
    if (!_intersects(first.outer(), other.outer())) {
      return false;
    }
    final Vector3d pos = other.position().subtract(first.position());
    for (int i = 0; i < 3; i++) {
      if (getSeparatingPlane(first, pos, first.axes()[i], other) || getSeparatingPlane(first, pos, other.axes()[i], other)) {
        return false;
      }
    }
    for (int i = 0; i < 3; i++) {
      for (int j = 0; j < 3; j++) {
        if (getSeparatingPlane(first, pos, first.axes()[i].cross(other.axes()[j]), other)) {
          return false;
        }
      }
    }
    return true;
  }


  // check if there's a separating plane in between the selected axes
  private static boolean getSeparatingPlane(OBBImpl first, Vector3d pos, Vector3d plane, OBBImpl other) {
    final double dot = abs(pos.dot(plane));
    final double x1 = abs((first.axes()[0].multiply(first.extents().x())).dot(plane));
    final double y1 = abs((first.axes()[1].multiply(first.extents().y())).dot(plane));
    final double z1 = abs((first.axes()[2].multiply(first.extents().z())).dot(plane));
    final double x2 = abs((other.axes()[0].multiply(other.extents().x())).dot(plane));
    final double y2 = abs((other.axes()[1].multiply(other.extents().y())).dot(plane));
    final double z2 = abs((other.axes()[2].multiply(other.extents().z())).dot(plane));
    return dot > x1 + y1 + z1 + x2 + y2 + z2;
  }

  private static boolean _intersects(Ray first, Ray other) {
    Vector3d cross = first.direction().cross(other.direction());
    if (cross.lengthSq() < Collider.EPSILON) {
      return first.contains(other.position()) || other.contains(first.position());
    }
    double planarFactor = other.position().subtract(first.position()).dot(cross);
    return abs(planarFactor) < Collider.EPSILON;
  }
}
