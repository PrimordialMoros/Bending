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

package me.moros.bending.protection;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.moros.bending.model.user.User;
import org.bukkit.block.Block;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A multi-layered cache used to check if a User can build in a specific block location
 */
public class ProtectionCache {
	private final Map<User, UserProtectionCache> userMap = new ConcurrentHashMap<>();

	public Optional<Boolean> canBuild(User user, Block block) {
		UserProtectionCache cache = userMap.computeIfAbsent(user, u -> new UserProtectionCache());
		return Optional.ofNullable(cache.blockCache.getIfPresent(block));
	}

	public void store(User user, Block block, boolean allowed) {
		UserProtectionCache cache = userMap.computeIfAbsent(user, u -> new UserProtectionCache());
		cache.blockCache.put(block, allowed);
	}

	public void invalidate(User user) {
		userMap.remove(user);
	}

	/**
	 * Represents an individual user's cache.
	 * Cached entries expire after 5000ms unless accessed again in which case the expiration time is refreshed.
	 */
	private static class UserProtectionCache {
		private final Cache<Block, Boolean> blockCache = Caffeine.newBuilder()
			.expireAfterAccess(Duration.ofMillis(5000))
			.build();
	}
}
