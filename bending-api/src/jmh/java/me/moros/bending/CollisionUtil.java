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

package me.moros.bending;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Rotation;
import me.moros.bending.model.math.Vector3d;

public class CollisionUtil {
  private static final Vector3d[] AXES = new Vector3d[]{Vector3d.PLUS_I, Vector3d.PLUS_J, Vector3d.PLUS_K};

  public static Collection<CachedAbility> generateColliders(int size) {
    return generateColliders(size, false, CollectionType.ArrayList);
  }

  public static Collection<CachedAbility> generateColliders(int size, boolean extraColliders) {
    return generateColliders(size, extraColliders, CollectionType.ArrayList);
  }

  public static Collection<CachedAbility> generateColliders(int size, boolean extraColliders, CollectionType type) {
    ThreadLocalRandom rand = ThreadLocalRandom.current();
    Collection<CachedAbility> result = type.create(3 * 20 * size);
    UUID[] uuids = new UUID[30 * size];
    for (int i = 0; i < uuids.length; i++) {
      uuids[i] = UUID.randomUUID();
    }
    for (int i = 0; i < 3; i++) {
      for (int j = 1; j <= size; j++) {
        double[] arr = new double[]{1, 1, 1};
        arr[i] = rand.nextDouble(0.5, j);
        AABB aabb = new AABB(new Vector3d(arr).negate(), new Vector3d(arr));
        double angle = rand.nextDouble(2 * Math.PI);
        Rotation rotation = new Rotation(AXES[i], angle);
        for (int k = 1; k <= 20; k++) {
          Vector3d center = randomVector(k * 0.5 * j);
          OBB obb = new OBB(aabb.at(center), rotation);
          UUID uuid = uuids[rand.nextInt(uuids.length)];
          List<Collider> colliders = new ArrayList<>();
          colliders.add(obb);
          if (extraColliders) {
            int extraAmount = rand.nextInt(-10, 10);
            for (int h = 0; h < extraAmount; h++) {
              double value = rand.nextDouble(0.2 * k);
              Vector3d center2 = randomVector(k * 0.5 * j);
              Vector3d center3 = randomVector(k * 0.5 * j);
              int r = rand.nextInt(3);
              Collider collider;
              if (r == 0) {
                collider = new Sphere(center2, value);
              } else if (r == 1) {
                collider = new Ray(center2, center3.normalize().multiply(value));
              } else {
                collider = new AABB(center2.min(center3), center2.max(center3));
              }
              colliders.add(collider);
            }
            Collections.shuffle(colliders);
          }
          result.add(new CachedAbility(uuid, List.copyOf(colliders)));
        }
      }
    }
    return result;
  }

  public static Vector3d randomVector(double spread) {
    double offsetX = ThreadLocalRandom.current().nextDouble(-spread, spread);
    double offsetY = ThreadLocalRandom.current().nextDouble(-spread, spread);
    double offsetZ = ThreadLocalRandom.current().nextDouble(-spread, spread);
    return new Vector3d(offsetX, offsetY, offsetZ);
  }

  public enum CollectionType {
    HashSet {
      @Override
      public <E> Collection<E> create(int maximumSize) {
        return new HashSet<>(maximumSize);
      }
    },
    IdentityHashSet {
      @Override
      public <E> Collection<E> create(int maximumSize) {
        return Collections.newSetFromMap(new IdentityHashMap<>(maximumSize));
      }
    },
    LinkedHashSet {
      @Override
      public <E> Collection<E> create(int maximumSize) {
        return new LinkedHashSet<>(maximumSize);
      }
    },
    ConcurrentHashSet {
      @Override
      public <E> Collection<E> create(int maximumSize) {
        return new LinkedHashSet<>(maximumSize);
      }
    },
    ArrayList {
      @Override
      public <E> Collection<E> create(int maximumSize) {
        return new ArrayList<>(maximumSize);
      }
    };

    public abstract <E> Collection<E> create(int maximumSize);
  }

  public record CachedAbility(UUID uuid, Collection<Collider> colliders) {
  }
}
