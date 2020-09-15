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

package me.moros.bending.listeners;

import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.util.DamageUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.PlayerInventory;

public class TempArmorListener implements Listener {
	@EventHandler(ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.isCancelled() || !(event.getClickedInventory() instanceof PlayerInventory) || event.getSlotType() != InventoryType.SlotType.ARMOR) {
			return;
		}
		PlayerInventory inventory = (PlayerInventory) event.getClickedInventory();
		if (inventory.getHolder() instanceof Player) {
			Player player = ((Player) inventory.getHolder()).getPlayer();
			if (TempArmor.manager.isTemp(player)) event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		TempArmor.manager.get(event.getEntity()).ifPresent(tempArmor -> {
			event.getDrops().removeIf(item -> tempArmor.getArmor().contains(item));
			event.getDrops().addAll(tempArmor.getSnapshot());
			tempArmor.revert();
			// TODO ensure drops don't get duplicated
		});
		String newMessage = DamageUtil.getBendingMessage(event.getEntity().getUniqueId());
		if (newMessage != null) event.setDeathMessage(newMessage);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlayerLogout(PlayerQuitEvent event) {
		TempArmor.manager.get(event.getPlayer()).ifPresent(TempArmor::revert);
	}
}
