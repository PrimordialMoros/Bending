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

package me.moros.bending.fabric.event;

import java.util.List;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ServerBlockEvents {
  private ServerBlockEvents() {
  }

  public static final Event<PistonMove> PISTON = EventFactory.createArrayBacked(PistonMove.class, callbacks -> (level, pos, toPush, toDestroy) -> {
    for (var callback : callbacks) {
      if (!callback.onPistonMove(level, pos, toPush, toDestroy)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<AfterBreak> AFTER_BREAK = EventFactory.createArrayBacked(AfterBreak.class, callbacks -> (level, pos) -> {
    for (var callback : callbacks) {
      if (!callback.onBreak(level, pos)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<Change> CHANGE = EventFactory.createArrayBacked(Change.class, callbacks -> (level, pos) -> {
    for (var callback : callbacks) {
      if (!callback.onChange(level, pos)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<Spread> SPREAD = EventFactory.createArrayBacked(Spread.class, callbacks -> (level, pos, pos2) -> {
    for (var callback : callbacks) {
      if (!callback.onSpread(level, pos, pos2)) {
        return false;
      }
    }
    return true;
  });

  @FunctionalInterface
  public interface PistonMove {
    boolean onPistonMove(ServerLevel level, BlockPos pos, List<BlockPos> toPush, List<BlockPos> toDestroy);
  }

  @FunctionalInterface
  public interface AfterBreak {
    boolean onBreak(ServerLevel level, BlockPos pos);
  }

  @FunctionalInterface
  public interface Change {
    boolean onChange(ServerLevel level, BlockPos pos);
  }

  @FunctionalInterface
  public interface Spread {
    boolean onSpread(ServerLevel level, BlockPos pos, BlockPos pos2);
  }
}
