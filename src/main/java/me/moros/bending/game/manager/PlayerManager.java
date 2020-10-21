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

import me.moros.atlas.caffeine.cache.AsyncLoadingCache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.Element;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.profile.BendingProfile;
import me.moros.bending.storage.BendingStorage;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class PlayerManager {
	private final Map<UUID, BendingPlayer> players = new ConcurrentHashMap<>();
	private final AsyncLoadingCache<UUID, BendingProfile> cache;

	public PlayerManager(@NonNull BendingStorage storage) {
		cache = Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(2)).buildAsync(storage::createProfile);
	}

	/**
	 * UUID must correspond to an online player
	 * @param uuid the uuid of the player object
	 * @return the BendingPlayer instance associated with the specified player
	 */
	public @NonNull BendingPlayer getPlayer(@NonNull UUID uuid) {
		return Objects.requireNonNull(players.get(uuid));
	}

	public @NonNull Collection<@NonNull BendingPlayer> getOnlinePlayers() {
		return players.values().stream().filter(BendingPlayer::isValid).collect(Collectors.toList());
	}

	public void invalidatePlayer(@NonNull UUID uuid) {
		players.remove(uuid);
		cache.synchronous().invalidate(uuid);
	}

	public boolean playerExists(@NonNull UUID uuid) {
		return players.containsKey(uuid);
	}

	public void createPlayer(@NonNull Player player, @NonNull BendingProfile profile) {
		BendingPlayer.createPlayer(player, profile).ifPresent(p -> {
			players.put(p.getProfile().getUniqueId(), p);
			p.getProfile().getData().elements.stream().map(Element::getElementByName).forEach(o -> o.ifPresent(p::addElement));
			p.bindPreset(new Preset(p.getProfile().getData().slots));
			Bending.getGame().getBoardManager().canUseScoreboard(p.getEntity());
			Bending.getGame().getAbilityManager(p.getWorld()).createPassives(p);
			Bending.getEventBus().postBendingPlayerLoadEvent(p);
		});
		Bending.getLog().info(profile.toString()); // TODO remove debug message
	}

	public Optional<BendingProfile> getProfile(@NonNull UUID uuid) {
		return Optional.ofNullable(cache.synchronous().get(uuid));
	}

	public CompletableFuture<BendingProfile> getProfileAsync(@NonNull UUID uuid) {
		return cache.getIfPresent(uuid);
	}
}
