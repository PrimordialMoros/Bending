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

import java.util.HashMap;
import java.util.Map;

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempBlock.Snapshot;
import me.moros.bending.api.user.User;

class SelectedSourceWithState extends SelectedSourceImpl implements SelectedSource.WithState {
  static final Map<Block, WithState> INSTANCES = new HashMap<>();

  private Snapshot snapshot;

  SelectedSourceWithState(User user, Block block, double maxDistance, BlockState state) {
    super(user, block, maxDistance);
    updateSnapshot(state);
  }

  @Override
  public boolean reselect(Block block, BlockState state) {
    if (super.reselect(block)) {
      updateSnapshot(state);
      return true;
    }
    return false;
  }

  private void updateSnapshot(BlockState state) {
    type = state.type();
    snapshot = TempBlock.MANAGER.get(block).map(TempBlock::snapshot).orElse(null);
    TempBlock.builder(state).build(block);
    INSTANCES.put(block, this);
  }

  @Override
  protected void render() {
  }

  @Override
  public void onDestroy() {
    if (block.type() == type) {
      TempBlock.revertToSnapshot(block, snapshot);
    }
    INSTANCES.remove(block);
  }
}
