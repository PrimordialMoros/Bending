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

package me.moros.bending.game;

import me.moros.bending.model.user.BendingUser;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

public final class BenderRegistry {
	private static Predicate<UUID> isBendingUser = (id -> false);
	private static Function<UUID, BendingUser> entityToUser = (id -> null);
	private static Function<String, BendingUser> nameToUser = (name -> null);

	public static boolean isBender(Entity entity) {
		if (entity instanceof Player) {
			return true;
		} else if (entity instanceof LivingEntity) {
			return isBendingUser.test(entity.getUniqueId());
		}
		return false;
	}

	public static Optional<BendingUser> getBendingUser(CommandSender entity) {
		if (entity instanceof Player) {
			return Optional.of(Game.getPlayerManager().getPlayer(((Player) entity).getUniqueId()));
		} else if (entity instanceof LivingEntity) {
			return Optional.ofNullable(entityToUser.apply(((LivingEntity) entity).getUniqueId()));
		}
		return Optional.empty();
	}

	public static Optional<BendingUser> getBendingUserByName(String name) {
		Player player = Bukkit.getPlayer(name);
		if (player != null) {
			return Optional.of(Game.getPlayerManager().getPlayer(player.getUniqueId()));
		}
		return Optional.ofNullable(nameToUser.apply(name));
	}

	public static void registerPredicate(Predicate<UUID> predicate) {
		isBendingUser = Objects.requireNonNull(predicate);
	}

	public static void registerCache(Function<UUID, BendingUser> function) {
		entityToUser = Objects.requireNonNull(function);
	}

	public static void registerNameCache(Function<String, BendingUser> function) {
		nameToUser = Objects.requireNonNull(function);
	}
}
