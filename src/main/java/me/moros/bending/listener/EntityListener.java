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
import me.moros.bending.ability.earth.*;
import me.moros.bending.game.Game;
import me.moros.bending.util.Metadata;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;

public class EntityListener implements Listener {
	private final Game game;

	public EntityListener(@NonNull Game game) {
		this.game = game;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onArrowHit(ProjectileHitEvent event) {
		if (event.getEntity() instanceof Arrow && event.getEntity().hasMetadata(Metadata.METAL_CABLE)) {
			MetalCable cable = (MetalCable) event.getEntity().getMetadata(Metadata.METAL_CABLE).get(0).value();
			if (cable != null) {
				if (event.getHitBlock() != null) {
					cable.setHitBlock(event.getHitBlock());
				} else if (event.getHitEntity() instanceof LivingEntity) {
					cable.setHitEntity(event.getHitEntity());
				} else {
					event.getEntity().remove();
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata(Metadata.METAL_CABLE)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onItemMerge(ItemMergeEvent event) {
		if (event.getEntity().hasMetadata(Metadata.GLOVE_KEY) || event.getTarget().hasMetadata(Metadata.GLOVE_KEY)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onItemPickup(EntityPickupItemEvent event) {
		if (event.getItem().hasMetadata(Metadata.NO_PICKUP)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onHopperItemPickup(InventoryPickupItemEvent event) {
		if (event.getItem().hasMetadata(Metadata.NO_PICKUP)) {
			event.setCancelled(true);
			event.getItem().remove();
		}
	}
}
