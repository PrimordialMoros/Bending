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

package me.moros.bending.model.temporal;

import me.moros.math.FastMath;

/**
 * Temporaries revert to their original state when their duration ends or when {@link #revert()} is manually called.
 */
@FunctionalInterface
public interface Temporary {
  /**
   * Default duration for temporaries in milliseconds equivalent to 10 minutes.
   */
  long DEFAULT_REVERT = 600_000;

  /**
   * Try to revert this temporary.
   * @return whether reverting was successfully completed
   */
  boolean revert();

  /**
   * Convert a duration from milliseconds to minecraft server ticks.
   * If duration is non-positive then {@link #DEFAULT_REVERT} will be used instead.
   * @param duration the duration in milliseconds
   * @return the converted ticks
   */
  static int toTicks(long duration) {
    return FastMath.ceil(duration <= 0 ? DEFAULT_REVERT : duration / 50.0);
  }
}
