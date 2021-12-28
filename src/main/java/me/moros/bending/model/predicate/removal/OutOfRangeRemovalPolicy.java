/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.model.predicate.removal;

import java.util.function.Supplier;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class OutOfRangeRemovalPolicy implements RemovalPolicy {
  private final double rangeSq;
  private final Vector3d origin;
  private final Supplier<Vector3d> fromSupplier;

  private OutOfRangeRemovalPolicy(double range, Vector3d origin, Supplier<Vector3d> from) {
    this.rangeSq = range * range;
    this.origin = origin;
    this.fromSupplier = from;
  }

  @Override
  public boolean test(@NonNull User user, @NonNull AbilityDescription desc) {
    if (rangeSq == 0) {
      return false;
    }
    return fromSupplier.get().distanceSq(origin == null ? user.eyeLocation() : origin) > rangeSq;
  }

  public static @NonNull RemovalPolicy of(double range, @NonNull Supplier<Vector3d> from) {
    return of(range, null, from);
  }

  public static @NonNull RemovalPolicy of(double range, @Nullable Vector3d origin, @NonNull Supplier<Vector3d> from) {
    return new OutOfRangeRemovalPolicy(range, origin, from);
  }
}
