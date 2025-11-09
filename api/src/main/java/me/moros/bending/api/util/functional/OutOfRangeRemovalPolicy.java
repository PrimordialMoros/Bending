/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.api.util.functional;

import java.util.function.Supplier;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.user.User;
import me.moros.math.Vector3d;
import org.jspecify.annotations.Nullable;

/**
 * Policy to remove ability when it reaches maximum range.
 */
public final class OutOfRangeRemovalPolicy implements RemovalPolicy {
  private final double rangeSq;
  private final Vector3d origin;
  private final Supplier<Vector3d> supplier;

  private OutOfRangeRemovalPolicy(double range, @Nullable Vector3d origin, Supplier<Vector3d> supplier) {
    this.rangeSq = range * range;
    this.origin = origin;
    this.supplier = supplier;
  }

  @Override
  public boolean test(User user, AbilityDescription desc) {
    return supplier.get().distanceSq(origin == null ? user.eyeLocation() : origin) > rangeSq;
  }

  /**
   * Creates a {@link RemovalPolicy} with a max range that checks the distance from {@link User#eyeLocation()}.
   * <p>Note: A range of 0 will be ignored and policy will always test negative.
   * @param range the maximum range of the ability
   * @param supplier supplier of location to measure distance to
   * @return the constructed policy
   */
  public static RemovalPolicy of(double range, Supplier<Vector3d> supplier) {
    return of(range, null, supplier);
  }

  /**
   * Creates a {@link RemovalPolicy} with a max range that checks the distance from given origin point.
   * <p>Note: A range of 0 will be ignored and policy will always test negative.
   * @param range the maximum range of the ability
   * @param origin the origin point to measure distance from, if null the policy will use the {@link User#eyeLocation()}
   * @param supplier supplier of location to measure distance to
   * @return the constructed policy
   */
  public static RemovalPolicy of(double range, @Nullable Vector3d origin, Supplier<Vector3d> supplier) {
    if (range == 0) {
      return (u, d) -> false;
    }
    return new OutOfRangeRemovalPolicy(range, origin, supplier);
  }
}
