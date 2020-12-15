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

package me.moros.bending.game.temporal;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class TempArmor implements Temporary {
	public static final TemporalManager<Player, TempArmor> manager = new TemporalManager<>();
	private final Player player;
	private final ItemStack[] snapshot;

	private RevertTask revertTask;

	public static void init() {
	}

	private TempArmor(Player player, ItemStack[] armor, long duration) {
		this.player = player;
		this.snapshot = copyFilteredArmor(player.getInventory().getArmorContents());
		player.getInventory().setArmorContents(applyMetaToArmor(armor));
		manager.addEntry(player, this, duration);
	}

	public static Optional<TempArmor> create(@NonNull Player player, @NonNull ItemStack[] armor, long duration) {
		if (manager.isTemp(player)) return Optional.empty();
		return Optional.of(new TempArmor(player, armor, duration));
	}

	public @NonNull Player getPlayer() {
		return player;
	}

	public @NonNull Collection<ItemStack> getSnapshot() {
		return Arrays.asList(snapshot);
	}

	@Override
	public void revert() {
		player.getInventory().setArmorContents(snapshot);
		manager.removeEntry(player);
		if (revertTask != null) revertTask.execute();
	}

	@Override
	public void setRevertTask(RevertTask task) {
		this.revertTask = task;
	}

	private ItemStack[] applyMetaToArmor(ItemStack[] armorItems) {
		for (ItemStack item : armorItems) {
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName("Bending Armor");
			meta.setLore(Collections.singletonList(ChatColor.GRAY + "Temporary"));
			meta.setUnbreakable(true);
			Bending.getLayer().addArmorKey(meta);
			item.setItemMeta(meta);
		}
		return armorItems;
	}

	private ItemStack[] copyFilteredArmor(ItemStack[] armorItems) {
		ItemStack[] copy = new ItemStack[armorItems.length];
		for (int i = 0; i < armorItems.length; i++) {
			ItemStack item = armorItems[i];
			if (item != null && item.getItemMeta() != null) {
				if (!Bending.getLayer().hasArmorKey(item.getItemMeta())) {
					copy[i] = item;
				}
			}
		}
		return copy;
	}
}
