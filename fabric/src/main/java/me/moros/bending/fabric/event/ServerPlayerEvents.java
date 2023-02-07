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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;

public final class ServerPlayerEvents {
  private ServerPlayerEvents() {
  }

  public static final Event<Interact> INTERACT = EventFactory.createArrayBacked(Interact.class, callbacks -> (player, hand) -> {
    for (var callback : callbacks) {
      var result = callback.onInteract(player, hand);
      if (result != InteractionResult.PASS) {
        return result;
      }
    }
    return InteractionResult.PASS;
  });

  public static final Event<Sneak> TOGGLE_SNEAK = EventFactory.createArrayBacked(Sneak.class, callbacks -> (player, sneaking) -> {
    for (var callback : callbacks) {
      if (!callback.onSneak(player, sneaking)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<Sprint> TOGGLE_SPRINT = EventFactory.createArrayBacked(Sprint.class, callbacks -> (player, sprinting) -> {
    for (var callback : callbacks) {
      if (!callback.onSprint(player, sprinting)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<GameMode> CHANGE_GAMEMODE = EventFactory.createArrayBacked(GameMode.class, callbacks -> (player, newGameMode) -> {
    for (var callback : callbacks) {
      callback.onGameModeChange(player, newGameMode);
    }
  });

  public static final Event<ChangeHeldSlot> CHANGE_SLOT = EventFactory.createArrayBacked(ChangeHeldSlot.class, callbacks -> (player, oldSlot, newSlot) -> {
    for (var callback : callbacks) {
      callback.onHeldSlotChange(player, oldSlot, newSlot);
    }
  });

  public static final Event<ModifyInventorySlot> MODIFY_INVENTORY_SLOT = EventFactory.createArrayBacked(ModifyInventorySlot.class, callbacks -> (player, item) -> {
    for (var callback : callbacks) {
      if (!callback.onModify(player, item)) {
        return false;
      }
    }
    return true;
  });

  public static final Event<PlaceBlock> PLACE_BLOCK = EventFactory.createArrayBacked(PlaceBlock.class, callbacks -> (player, pos, state) -> {
    for (var callback : callbacks) {
      if (!callback.onPlace(player, pos, state)) {
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

  @FunctionalInterface
  public interface GameMode {
    void onGameModeChange(ServerPlayer player, GameType newGameMode);
  }

  @FunctionalInterface
  public interface ChangeHeldSlot {
    void onHeldSlotChange(ServerPlayer player, int oldSlot, int newSlot);
  }

  @FunctionalInterface
  public interface ModifyInventorySlot {
    boolean onModify(ServerPlayer player, ItemStack item);
  }

  @FunctionalInterface
  public interface PlaceBlock {
    boolean onPlace(ServerPlayer player, BlockPos pos, BlockState state);
  }
}
