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

package me.moros.bending.api.util.functional;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.user.User;

/**
 * Policy to remove ability after a specific duration of time.
 */
public final class ExpireRemovalPolicy implements RemovalPolicy {
  private final long expireTime;

  private ExpireRemovalPolicy(long duration) {
    expireTime = System.currentTimeMillis() + duration;
  }

  @Override
  public boolean test(User user, AbilityDescription desc) {
    return System.currentTimeMillis() > expireTime;
  }

  /**
   * Creates a {@link RemovalPolicy} with a maximum duration.
   * <p>Note: A non-positive duration will be ignored and policy will always test negative.
   * @param duration the maximum duration
   * @return the constructed policy
   */
  public static RemovalPolicy of(long duration) {
    if (duration <= 0) {
      return (u, d) -> false;
    }
    return new ExpireRemovalPolicy(duration);
  }
}
