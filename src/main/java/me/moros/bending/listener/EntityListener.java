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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.ability.earth.MetalCable;
import me.moros.bending.game.Game;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.util.ActionType;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.MovementHandler;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
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
	public void onEntityExplodeEvent(EntityExplodeEvent event) {
		if (MovementHandler.isRestricted(event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityInteractEvent(EntityInteractEvent event) {
		if (MovementHandler.isRestricted(event.getEntity(), ActionType.INTERACT)) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onProjectileLaunchEvent(ProjectileLaunchEvent event) {
		if (MovementHandler.isRestricted(event.getEntity(), ActionType.SHOOT)) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityShootBowEvent(EntityShootBowEvent event) {
		if (MovementHandler.isRestricted(event.getEntity(), ActionType.SHOOT)) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onSlimeSplitEvent(SlimeSplitEvent event) {
		if (MovementHandler.isRestricted(event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityTarget(EntityTargetEvent event) {
		if (MovementHandler.isRestricted(event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityTargetLiving(EntityTargetLivingEntityEvent event) {
		if (MovementHandler.isRestricted(event.getEntity())) event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() == EntityDamageEvent.DamageCause.SUFFOCATION && event.getEntity() instanceof LivingEntity) {
			if (TempBlock.MANAGER.isTemp(((LivingEntity) event.getEntity()).getEyeLocation().getBlock())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Arrow && event.getDamager().hasMetadata(Metadata.METAL_CABLE)) {
			event.setCancelled(true);
		}
		if (MovementHandler.isRestricted(event.getDamager(), ActionType.DAMAGE)) event.setCancelled(true);
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
