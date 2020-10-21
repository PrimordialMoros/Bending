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

package me.moros.bending.util;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.taskchain.BukkitTaskChainFactory;
import me.moros.atlas.taskchain.TaskChain;
import me.moros.atlas.taskchain.TaskChainFactory;
import me.moros.bending.Bending;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

/**
 * Utility class to provide task chains and create repeating tasks.
 * @see TaskChain
 */
public final class Tasker {
	private static TaskChainFactory taskChainFactory;

	public static void init(Bending plugin) {
		if (taskChainFactory == null) taskChainFactory = BukkitTaskChainFactory.create(plugin);
	}

	public static <T> TaskChain<T> newChain() {
		return taskChainFactory.newChain();
	}

	public static <T> TaskChain<T> newSharedChain(@NonNull String name) {
		return taskChainFactory.newSharedChain(name);
	}

	public static BukkitTask createTaskTimer(@NonNull Runnable runnable, long delay, long period) {
		return Bukkit.getScheduler().runTaskTimer(Bending.getPlugin(), runnable, delay, period);
	}
}
