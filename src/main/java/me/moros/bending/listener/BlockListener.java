/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.material.WaterMaterials;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class BlockListener implements Listener {
	private final Game game;

	public BlockListener(@NonNull Game game) {
		this.game = game;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {
		if (TempBlock.manager.isTemp(event.getIgnitingBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockSpread(BlockSpreadEvent event) {
		if (TempBlock.manager.isTemp(event.getSource())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockFade(BlockFadeEvent event) {
		if (TempBlock.manager.isTemp(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {
		if (TempBlock.manager.isTemp(event.getIgnitingBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {
		TempBlock.manager.get(event.getBlock()).ifPresent(TempBlock::removeWithoutReverting);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		if (TempBlock.manager.isTemp(event.getBlock())) {
			event.setDropItems(false);
		} else if (WaterMaterials.PLANT_BENDABLE.isTagged(event.getBlock())) {
			BendingPlayer player = game.getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
			player.getSelectedAbility().ifPresent(desc -> {
				if (desc.canSourcePlant() && !player.isOnCooldown(desc)) {
					event.setCancelled(true);
				}
			});
		}
		TempBlock.manager.get(event.getBlock()).ifPresent(TempBlock::removeWithoutReverting);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockForm(BlockFormEvent event) {
		if (TempBlock.manager.isTemp(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBlockFromTo(BlockFromToEvent event) {
		if (TempBlock.manager.isTemp(event.getBlock()) || TempBlock.manager.isTemp(event.getToBlock())) {
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
			BendingFallingBlock.manager.get(fb).ifPresent(BendingFallingBlock::revert);
		}
	}
}
