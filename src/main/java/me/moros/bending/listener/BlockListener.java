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

package me.moros.bending.listener;

import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempFallingBlock;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.MovementHandler;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.checkerframework.checker.nullness.qual.NonNull;

public class BlockListener implements Listener {
  private final Game game;

  public BlockListener(@NonNull Game game) {
    this.game = game;
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockIgnite(BlockIgniteEvent event) {
    if (TempBlock.MANAGER.isTemp(event.getIgnitingBlock())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockSpread(BlockSpreadEvent event) {
    if (TempBlock.MANAGER.isTemp(event.getSource())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockFade(BlockFadeEvent event) {
    Block block = event.getBlock();
    if (!MaterialUtil.isFire(block) && TempBlock.MANAGER.isTemp(block)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockBurn(BlockBurnEvent event) {
    if (TempBlock.MANAGER.isTemp(event.getIgnitingBlock())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (MovementHandler.isRestricted(event.getPlayer(), ActionType.INTERACT_BLOCK)) {
      event.setCancelled(true);
      return;
    }
    TempBlock.MANAGER.get(event.getBlock()).ifPresent(TempBlock::removeWithoutReverting);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    if (TempBlock.MANAGER.isTemp(event.getBlock())) {
      event.setDropItems(false);
    } else if (WaterMaterials.isPlantBendable(event.getBlock())) {
      BendingPlayer player = Registries.BENDERS.user(event.getPlayer());
      AbilityDescription desc = player.selectedAbility();
      if (desc != null && desc.sourcePlant() && !player.onCooldown(desc)) {
        event.setCancelled(true);
        return;
      }
    }
    TempBlock.MANAGER.get(event.getBlock()).ifPresent(TempBlock::removeWithoutReverting);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockForm(BlockFormEvent event) {
    if (TempBlock.MANAGER.isTemp(event.getBlock())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockFromTo(BlockFromToEvent event) {
    if (TempBlock.MANAGER.isTemp(event.getBlock()) || TempBlock.MANAGER.isTemp(event.getToBlock())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPhysics(BlockPhysicsEvent event) {
    Block block = event.getBlock();
    if (block.getType().hasGravity() && TempBlock.shouldIgnorePhysics(block)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockChange(EntityChangeBlockEvent event) {
    if (event.getEntityType() == EntityType.FALLING_BLOCK) {
      FallingBlock fb = (FallingBlock) event.getEntity();
      if (fb.hasMetadata(Metadata.FALLING_BLOCK)) {
        event.setCancelled(true);
      }
      TempFallingBlock.MANAGER.get(fb).ifPresent(TempFallingBlock::revert);
    } else {
      if (MovementHandler.isRestricted(event.getEntity(), ActionType.INTERACT_BLOCK)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
    if (event.getBlocks().stream().anyMatch(TempBlock.MANAGER::isTemp)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
    if (event.getBlocks().stream().anyMatch(TempBlock.MANAGER::isTemp)) {
      event.setCancelled(true);
    }
  }
}
