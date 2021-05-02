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

package me.moros.bending.ability.common.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Supplier;

import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Burstable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.methods.EntityMethods;
import org.apache.commons.math3.util.FastMath;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractBurst extends AbilityInstance {
  protected double angleStep = FastMath.toRadians(10);
  protected double angle = FastMath.toRadians(30);

  protected final Collection<Burstable> blasts = new ArrayList<>();

  protected AbstractBurst(@NonNull AbilityDescription desc) {
    super(desc);
  }

  protected <T extends Burstable> void cone(@NonNull Supplier<T> constructor, double range) {
    createBurst(constructor, range, new Angles(), true);
  }

  protected <T extends Burstable> void sphere(@NonNull Supplier<T> constructor, double range) {
    createBurst(constructor, range, new Angles(), false);
  }

  protected <T extends Burstable> void fall(@NonNull Supplier<T> constructor, double range) {
    createBurst(constructor, range, new Angles(FastMath.toRadians(75), FastMath.toRadians(105)), false);
  }

  private <T extends Burstable> void createBurst(Supplier<T> constructor, double range, Angles angles, boolean cone) {
    User user = user();
    Vector3 center = EntityMethods.entityCenter(user.entity());
    Vector3 userDIr = user.direction();
    for (double theta = angles.minTheta; theta < angles.maxTheta; theta += angleStep) {
      for (double phi = angles.minPhi; phi < angles.maxPhi; phi += angleStep) {
        double x = FastMath.cos(phi) * FastMath.sin(theta);
        double y = FastMath.cos(phi) * FastMath.cos(theta);
        double z = FastMath.sin(phi);
        Vector3 direction = new Vector3(x, y, z);
        if (cone && Vector3.angle(direction, userDIr) > angle) {
          continue;
        }
        T blast = constructor.get();
        Ray ray = new Ray(center, direction.scalarMultiply(range));
        blast.burstInstance(user, ray);
        blasts.add(blast);
      }
    }
  }

  protected @NonNull UpdateResult updateBurst() {
    blasts.removeIf(b -> b.update() == UpdateResult.REMOVE);
    return blasts.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  private static class Angles {
    private final double minTheta;
    private final double maxTheta;
    private final double minPhi;
    private final double maxPhi;

    private Angles() {
      this(0, FastMath.PI, 0, FastMath.PI * 2);
    }

    private Angles(double minTheta, double maxTheta) {
      this(minTheta, maxTheta, 0, FastMath.PI * 2);
    }

    private Angles(double minTheta, double maxTheta, double minPhi, double maxPhi) {
      this.minTheta = minTheta;
      this.maxTheta = maxTheta;
      this.minPhi = minPhi;
      this.maxPhi = maxPhi;
    }
  }
}
