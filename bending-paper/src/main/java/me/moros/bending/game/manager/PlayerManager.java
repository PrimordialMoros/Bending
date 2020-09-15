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

package me.moros.bending.game.manager;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.moros.bending.Bending;
import me.moros.bending.board.BoardManager;
import me.moros.bending.game.Game;
import me.moros.bending.model.user.player.BendingProfile;
import me.moros.bending.model.Element;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.preset.Preset;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PlayerManager {
	private final Map<UUID, BendingPlayer> players = new ConcurrentHashMap<>();
	private final AsyncLoadingCache<UUID, BendingProfile> cache = Caffeine.newBuilder()
		.maximumSize(100)
		.expireAfterWrite(Duration.ofMinutes(2))
		.buildAsync(Game.getStorage()::createProfile);

	/**
	 * UUID must correspond to an online player
	 *
	 * @param uuid the uuid of the player object
	 * @return the BendingPlayer instance associated with the specified player
	 */
	public BendingPlayer getPlayer(UUID uuid) {
		return Objects.requireNonNull(players.get(uuid));
	}

	public List<BendingPlayer> getOnlinePlayers() {
		return players.values().stream().filter(BendingPlayer::isValid).collect(Collectors.toList());
	}

	public void invalidatePlayer(BendingPlayer bendingPlayer) {
		UUID uuid = bendingPlayer.getProfile().getUniqueId();
		Game.getProtectionSystem().invalidate(bendingPlayer);
		BoardManager.invalidate(uuid);
		players.remove(uuid);
		cache.synchronous().invalidate(uuid);
	}

	public boolean playerExists(UUID uuid) {
		if (uuid == null) return false;
		return players.containsKey(uuid);
	}

	public void createPlayer(Player player, BendingProfile profile) {
		BendingPlayer.createPlayer(player, profile).ifPresent(p -> {
			players.put(player.getUniqueId(), p);
			profile.getData().elements.stream().map(Element::getElementByName).forEach(o -> o.ifPresent(p::addElement));
			p.bindPreset(new Preset(0, "temp", profile.getData().slots));
			BoardManager.canUseScoreboard(p.getEntity());
			Game.getAbilityInstanceManager(p.getWorld()).createPassives(p);
			Bending.getEventBus().postBendingPlayerLoadEvent(p.getEntity());
		});
		Bending.getLog().info(profile.toString()); // TODO remove debug message
	}

	public Optional<BendingProfile> getProfile(UUID uuid) {
		return Optional.ofNullable(cache.synchronous().get(uuid));
	}

	public CompletableFuture<BendingProfile> getProfileAsync(UUID uuid) {
		return cache.getIfPresent(uuid);
	}
}
