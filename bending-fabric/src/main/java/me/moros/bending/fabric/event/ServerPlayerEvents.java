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

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

public final class ServerPlayerEvents {
  private ServerPlayerEvents() {
  }

  public static final Event<Interact> INTERACT = EventFactory.createArrayBacked(Interact.class, callbacks -> (player, hand) -> {
    for (Interact callback : callbacks) {
      var result = callback.onInteract(player, hand);
      if (result != InteractionResult.PASS) {
        return result;
      }
    }
    return InteractionResult.PASS;
  });

  public static final Event<Sneak> TOGGLE_SNEAK = EventFactory.createArrayBacked(Sneak.class, callbacks -> (player, sneaking) -> {
    for (Sneak callback : callbacks) {
      if (!callback.onSneak(player, sneaking)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<Sprint> TOGGLE_SPRINT = EventFactory.createArrayBacked(Sprint.class, callbacks -> (player, sprinting) -> {
    for (Sprint callback : callbacks) {
      if (!callback.onSprint(player, sprinting)) {
        return false;
      }
    }
    return true;
  });

  @FunctionalInterface
  public interface Interact {
    InteractionResult onInteract(ServerPlayer player, InteractionHand hand);
  }

  @FunctionalInterface
  public interface Sneak {
    boolean onSneak(ServerPlayer player, boolean sneaking);
  }

  @FunctionalInterface
  public interface Sprint {
    boolean onSprint(ServerPlayer player, boolean sprinting);
  }
}
