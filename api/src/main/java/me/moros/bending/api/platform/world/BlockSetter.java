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

package me.moros.bending.api.platform.world;

import me.moros.bending.api.platform.block.BlockState;
import me.moros.math.Position;

public interface BlockSetter {
  default boolean setBlockState(Position position, BlockState state) {
    return setBlockState(position.blockX(), position.blockY(), position.blockZ(), state);
  }

  boolean setBlockState(int x, int y, int z, BlockState state);

  default boolean setBlockStateFast(Position position, BlockState state) {
    return setBlockStateFast(position.blockX(), position.blockY(), position.blockZ(), state);
  }

  boolean setBlockStateFast(int x, int y, int z, BlockState state);

  default boolean breakNaturally(Position position) {
    return breakNaturally(position.blockX(), position.blockY(), position.blockZ());
  }

  boolean breakNaturally(int x, int y, int z);
}
