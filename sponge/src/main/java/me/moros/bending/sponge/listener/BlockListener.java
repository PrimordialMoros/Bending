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

package me.moros.bending.sponge.listener;

import me.moros.bending.api.ability.ActionType;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.sponge.platform.PlatformAdapter;
import org.spongepowered.api.block.transaction.Operations;
import org.spongepowered.api.entity.FallingBlock;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.filter.cause.First;
import org.spongepowered.api.event.item.inventory.DropItemEvent;
import org.spongepowered.api.world.LocatableBlock;

public class BlockListener extends SpongeListener {
  public BlockListener(Game game) {
    super(game);
  }

  // TODO better event filtering, api block changes also fire events, messing a bunch of systems
  @Listener(order = Order.EARLY)
  public void onBlockChangePre(ChangeBlockEvent.Pre event) {
    if (disabledWorld(event.world())) {
      return;
    }
    var living = event.cause().first(Living.class).orElse(null);
    if (living != null && ActionLimiter.isLimited(living.uniqueId(), ActionType.INTERACT_BLOCK)) {
      event.setCancelled(true);
      return;
    }
    var blockCause = event.cause().first(LocatableBlock.class).orElse(null);
    for (var loc : event.locations()) {
      var block = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
      if (blockCause != null && TempBlock.MANAGER.isTemp(block)) {
        event.setCancelled(true);
        return;
      }
    }
  }

  @Listener(order = Order.EARLY)
  public void onBlockChange(ChangeBlockEvent.All event, @First Living entity) {
    if (disabledWorld(event.world())) {
      return;
    }
    for (var transaction : event.transactions()) {
      var loc = transaction.original().location().orElse(null);
      if (!transaction.isValid() || loc == null) {
        continue;
      }
      var op = transaction.operation();
      var block = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
      if (op == Operations.PLACE.get() || op == Operations.BREAK.get()) {
        TempBlock.MANAGER.get(block).ifPresent(TempBlock::removeWithoutReverting);
      } else {
        transaction.setValid(TempBlock.MANAGER.isTemp(block));
      }
    }
  }

  @Listener(order = Order.EARLY)
  public void onBlockDropItem(DropItemEvent.Destruct event) { // TODO needs testing
    var snapshot = event.context().get(EventContextKeys.BLOCK_TARGET).orElse(null);
    var loc = snapshot == null ? null : snapshot.location().orElse(null);
    if (loc == null || disabledWorld(loc.world())) {
      return;
    }
    var block = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
    if (TempBlock.MANAGER.isTemp(block)) {
      event.setCancelled(true);
    }
  }

  @Listener(order = Order.FIRST)
  public void onBlockPhysics(ChangeBlockEvent.All event, @First FallingBlock fallingBlock) {
    if (disabledWorld(event.world())) {
      return;
    }
    var type = PlatformAdapter.fromSpongeBlock(fallingBlock.blockState().get().type());
    if (type.hasGravity()) {
      for (var transaction : event.transactions()) {
        var loc = transaction.original().location().orElse(null);
        if (!transaction.isValid() || loc == null) {
          continue;
        }
        var block = PlatformAdapter.fromSpongeWorld(loc.world()).blockAt(loc.blockX(), loc.blockY(), loc.blockZ());
        if (TempBlock.shouldIgnorePhysics(block)) {
          transaction.invalidate();
        }
      }
    }
  }
}
