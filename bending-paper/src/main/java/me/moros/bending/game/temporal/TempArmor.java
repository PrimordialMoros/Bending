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

import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public class TempArmor implements Temporary {
	public static final TemporalManager<Player, TempArmor> manager = new TemporalManager<>();
	private final Player player;
	private final ItemStack[] snapshot;
	private final ItemStack[] armor;

	private RevertTask revertTask;

	public static void init() {
	}

	private TempArmor(Player player, ItemStack[] armor, long duration) {
		this.player = player;
		this.snapshot = player.getInventory().getArmorContents().clone();
		this.armor = armor.clone();
		player.getInventory().setArmorContents(this.armor);
		manager.addEntry(player, this, duration);
	}

	public static Optional<TempArmor> create(Player player, ItemStack[] armor, long duration) {
		if (manager.isTemp(player)) return Optional.empty();
		return Optional.of(new TempArmor(player, armor, duration));
	}

	public Player getPlayer() {
		return player;
	}

	public Collection<ItemStack> getSnapshot() {
		return Arrays.asList(snapshot);
	}

	public Collection<ItemStack> getArmor() {
		return Arrays.asList(armor);
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
}
