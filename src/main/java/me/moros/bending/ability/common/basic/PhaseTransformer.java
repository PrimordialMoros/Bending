/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.common.basic;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;

import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class PhaseTransformer {
  private final Deque<Block> queue;

  public PhaseTransformer() {
    queue = new ArrayDeque<>(32);
  }

  public boolean fillQueue(@NonNull Collection<@NonNull Block> blocks) {
    return queue.addAll(blocks);
  }

  public void processQueue(int amount) {
    int counter = 0;
    while (!queue.isEmpty() && counter <= amount) {
      if (processBlock(queue.poll())) {
        counter++;
      }
    }
  }

  public void clear() {
    queue.clear();
  }

  protected abstract boolean processBlock(@NonNull Block block);
}
