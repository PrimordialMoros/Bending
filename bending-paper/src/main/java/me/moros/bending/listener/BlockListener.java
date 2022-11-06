/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.listener;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.ActionType;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import net.kyori.adventure.text.Component;
import org.bukkit.Nameable;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Lockable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;

public class BlockListener implements Listener {
  private final Game game;

  public BlockListener(Game game) {
    this.game = game;
  }

  private boolean disabledWorld(BlockEvent event) {
    return !game.worldManager().isEnabled(event.getBlock().getWorld());
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockIgnite(BlockIgniteEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (TempBlock.MANAGER.isTemp(event.getIgnitingBlock())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockSpread(BlockSpreadEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (TempBlock.MANAGER.isTemp(event.getSource())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockFade(BlockFadeEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    Block block = event.getBlock();
    if (!MaterialUtil.isFire(block) && TempBlock.MANAGER.isTemp(block)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockBurn(BlockBurnEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (TempBlock.MANAGER.isTemp(event.getIgnitingBlock())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPlace(BlockPlaceEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (ActionLimiter.isLimited(event.getPlayer(), ActionType.INTERACT_BLOCK)) {
      event.setCancelled(true);
      return;
    }
    TempBlock.MANAGER.get(event.getBlock()).ifPresent(TempBlock::removeWithoutReverting);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    Block block = event.getBlock();
    if (handleLockedContainer(event.getPlayer(), block)) {
      event.setCancelled(true);
      return;
    }
    if (disabledWorld(event)) {
      return;
    }
    if (TempBlock.MANAGER.isTemp(block)) {
      event.setDropItems(false);
    } else if (WaterMaterials.isPlantBendable(block)) {
      User user = Registries.BENDERS.get(event.getPlayer().getUniqueId());
      if (user != null) {
        AbilityDescription desc = user.selectedAbility();
        if (desc != null && desc.sourcePlant() && user.canBend(desc)) {
          event.setCancelled(true);
          return;
        }
      }
    }
    TempBlock.MANAGER.get(block).ifPresent(TempBlock::removeWithoutReverting);
  }

  private boolean handleLockedContainer(Player player, Block block) {
    if (block.getState(false) instanceof Lockable lockable && !WorldUtil.canBreak(player, lockable)) {
      Component name = ((Nameable) lockable).customName();
      if (name == null) {
        name = Component.translatable(block.translationKey());
      }
      player.sendActionBar(Component.translatable("container.isLocked").args(name));
      SoundUtil.of(Sound.BLOCK_CHEST_LOCKED).play(block);
      return true;
    }
    return false;
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockForm(BlockFormEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (TempBlock.MANAGER.isTemp(event.getBlock())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockFromTo(BlockFromToEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (TempBlock.MANAGER.isTemp(event.getBlock()) || TempBlock.MANAGER.isTemp(event.getToBlock())) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPhysics(BlockPhysicsEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    Block block = event.getBlock();
    if (block.getType().hasGravity() && TempBlock.shouldIgnorePhysics(block)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.getBlocks().stream().anyMatch(TempBlock.MANAGER::isTemp)) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
    if (disabledWorld(event)) {
      return;
    }
    if (event.getBlocks().stream().anyMatch(TempBlock.MANAGER::isTemp)) {
      event.setCancelled(true);
    }
  }
}
