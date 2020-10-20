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

import me.moros.bending.Bending;
import me.moros.bending.game.manager.PlayerManager;
import me.moros.bending.model.user.BendingUser;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * API for checking if an entity is a BendingUser
 * If you only need BendingPlayers then use {@link PlayerManager}
 */
public final class BenderRegistry {
	private Predicate<UUID> isBendingUser = (id -> false);
	private Function<UUID, BendingUser> entityToUser = (id -> null);
	private Function<String, BendingUser> nameToUser = (name -> null);

	public boolean isBender(@NonNull LivingEntity entity) {
		if (entity instanceof Player) {
			return true;
		}
		return isBendingUser.test(entity.getUniqueId());
	}

	public Optional<BendingUser> getBendingUser(@NonNull LivingEntity entity) {
		if (entity instanceof Player) {
			return Optional.of(Bending.getGame().getPlayerManager().getPlayer(entity.getUniqueId()));
		}
		return Optional.ofNullable(entityToUser.apply(entity.getUniqueId()));
	}

	public Optional<BendingUser> getBendingUserByName(@NonNull String name) {
		Player player = Bukkit.getPlayer(name);
		if (player != null) {
			return Optional.of(Bending.getGame().getPlayerManager().getPlayer(player.getUniqueId()));
		}
		return Optional.ofNullable(nameToUser.apply(name));
	}

	public void registerPredicate(@NonNull Predicate<@NonNull UUID> predicate) {
		isBendingUser = predicate;
	}

	public void registerCache(@NonNull Function<@NonNull UUID, @Nullable BendingUser> function) {
		entityToUser = function;
	}

	public void registerNameCache(@NonNull Function<@NonNull String, @Nullable BendingUser> function) {
		nameToUser = function;
	}
}
