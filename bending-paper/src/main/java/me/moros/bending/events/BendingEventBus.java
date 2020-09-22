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

package me.moros.bending.events;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BendingEventBus {
	public void postBendingPlayerLoadEvent(Player player) {
		Bukkit.getPluginManager().callEvent(new BendingPlayerLoadEvent(player));
	}

	public void postCooldownAddEvent(User user, AbilityDescription ability) {
		Bukkit.getPluginManager().callEvent(new CooldownAddEvent(user, ability));
	}

	public void postCooldownRemoveEvent(User user, AbilityDescription ability) {
		if (!user.isValid()) return; // We post the event 1 tick later so this is needed for safety
		Bukkit.getPluginManager().callEvent(new CooldownRemoveEvent(user, ability));
	}

	public void postElementChangeEvent(User user, ElementChangeEvent.Result result) {
		Bukkit.getPluginManager().callEvent(new ElementChangeEvent(user, result));
	}

	public void postBindChangeEvent(User user, BindChangeEvent.Result result) {
		Bukkit.getPluginManager().callEvent(new BindChangeEvent(user, result));
	}
}
