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

package me.moros.bending.api.ability.common;

import me.moros.bending.api.ability.state.State;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.user.User;

/**
 * State implementation for focusing a selected source.
 */
public interface SelectedSource extends State {
  Block block();

  boolean reselect(Block block);

  default void onDestroy() {
  }

  interface WithState extends SelectedSource {
    boolean reselect(Block block, BlockState state);
  }

  static void tryRevertSource(Block block) {
    SelectedSource selectedSource = SelectedSourceWithState.INSTANCES.get(block);
    if (selectedSource != null) {
      selectedSource.onDestroy();
    }
  }

  static SelectedSource create(User user, Block block, double maxDistance) {
    return new SelectedSourceImpl(user, block, maxDistance);
  }

  static SelectedSource.WithState create(User user, Block block, double maxDistance, BlockState state) {
    return new SelectedSourceWithState(user, block, maxDistance, state);
  }
}
