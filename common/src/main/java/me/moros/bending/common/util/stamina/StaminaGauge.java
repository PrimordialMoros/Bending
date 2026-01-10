/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.common.util.stamina;

final class StaminaGauge {
  private final int max;
  private int stamina;

  StaminaGauge(int max, int stamina) {
    this.max = max;
    this.stamina = Math.clamp(stamina, 0, max);
  }

  public float progress() {
    return Math.clamp(stamina / (float) max, 0, 1);
  }

  public boolean hasAtLeast(int amount) {
    return stamina >= amount;
  }

  public boolean decrement(int amount) {
    if (amount > 0 && stamina >= amount) {
      stamina -= amount;
      return true;
    }
    return false;
  }

  public boolean increment(int amount) {
    if (amount > 0 && stamina < max) {
      stamina = Math.min(stamina + amount, max);
      return true;
    }
    return false;
  }

  public boolean isEmpty() {
    return stamina <= 0;
  }

  public boolean isFull() {
    return stamina >= max;
  }
}
