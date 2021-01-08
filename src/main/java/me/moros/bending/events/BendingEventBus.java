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

package me.moros.bending.events;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import org.bukkit.Bukkit;

public class BendingEventBus {
	public void postBendingPlayerLoadEvent(@NonNull BendingPlayer player) {
		Bukkit.getPluginManager().callEvent(new BendingPlayerLoadEvent(player));
	}

	public void postCooldownAddEvent(@NonNull User user, @NonNull AbilityDescription desc, long duration) {
		Bukkit.getPluginManager().callEvent(new CooldownAddEvent(user, desc, duration));
	}

	public void postCooldownRemoveEvent(@NonNull User user, @NonNull AbilityDescription desc) {
		if (!user.isValid()) return; // We post the event 1 tick later so this is needed for safety
		Bukkit.getPluginManager().callEvent(new CooldownRemoveEvent(user, desc));
	}

	public void postElementChangeEvent(@NonNull User user, ElementChangeEvent.Result result) {
		Bukkit.getPluginManager().callEvent(new ElementChangeEvent(user, result));
	}

	public void postBindChangeEvent(@NonNull User user, BindChangeEvent.Result result) {
		Bukkit.getPluginManager().callEvent(new BindChangeEvent(user, result));
	}
}
