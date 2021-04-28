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

package me.moros.bending.model.collision.geometry;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.math.Vector3;

public class Ray {
  public final Vector3 origin, direction, invDir;

  public Ray(@NonNull Vector3 origin, @NonNull Vector3 direction) {
    this.origin = origin;
    this.direction = direction;
    double invX = direction.getX() == 0 ? Double.MAX_VALUE : 1 / direction.getX();
    double invY = direction.getX() == 0 ? Double.MAX_VALUE : 1 / direction.getY();
    double invZ = direction.getX() == 0 ? Double.MAX_VALUE : 1 / direction.getZ();
    invDir = new Vector3(invX, invY, invZ);
  }
}
