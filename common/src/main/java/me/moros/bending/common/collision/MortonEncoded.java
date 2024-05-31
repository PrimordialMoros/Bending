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

package me.moros.bending.common.collision;

import me.moros.math.Position;

public interface MortonEncoded {
  int morton();

  static int calculateMorton(Position position) {
    return calculateMorton(position.x(), position.y(), position.z());
  }

  static int calculateMorton(double x, double y, double z) {
    int normalizedX = (int) Math.clamp((x + 30_000_000) * 10, 0, 600_000_000);
    int normalizedY = (int) Math.clamp((y + 30_000_000) * 10, 0, 600_000_000);
    int normalizedZ = (int) Math.clamp((z + 30_000_000) * 10, 0, 600_000_000);
    int xx = expandBits(normalizedX);
    int yy = expandBits(normalizedY);
    int zz = expandBits(normalizedZ);
    return xx * 4 + yy * 2 + zz;
  }

  private static int expandBits(int v) {
    v = (v | v << 16) & 0xFF0000FF;
    v = (v | v << 8) & 0x0F00F00F;
    v = (v | v << 4) & 0xC30C30C3;
    v = (v | v << 2) & 0x49249249;
    return v;
  }
}
