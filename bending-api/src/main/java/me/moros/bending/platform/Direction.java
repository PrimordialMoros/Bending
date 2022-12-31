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

package me.moros.bending.platform;

import me.moros.math.Vector3i;

public enum Direction implements Vector3i {
  UP(Vector3i.PLUS_J),
  DOWN(Vector3i.MINUS_J),
  NORTH(Vector3i.MINUS_K),
  SOUTH(Vector3i.PLUS_K),
  EAST(Vector3i.PLUS_I),
  WEST(Vector3i.MINUS_I);

  private final Vector3i vector;

  Direction(Vector3i vector) {
    this.vector = vector;
  }

  @Override
  public int blockX() {
    return vector.blockX();
  }

  @Override
  public int blockY() {
    return vector.blockY();
  }

  @Override
  public int blockZ() {
    return vector.blockZ();
  }

  public Direction opposite() {
    return switch (this) {
      case UP -> DOWN;
      case DOWN -> UP;
      case EAST -> WEST;
      case WEST -> EAST;
      case NORTH -> SOUTH;
      case SOUTH -> NORTH;
    };
  }
}
