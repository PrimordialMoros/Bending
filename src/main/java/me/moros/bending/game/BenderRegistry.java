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

package me.moros.bending.game;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import me.moros.atlas.caffeine.cache.AsyncLoadingCache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.bending.Bending;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.BendingUser;
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.model.user.profile.BendingProfile;
import me.moros.bending.storage.BendingStorage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Registry for all valid benders
 */
public final class BenderRegistry {
  private final AsyncLoadingCache<UUID, BendingProfile> cache;
  private final Map<UUID, BendingPlayer> players;
  private final Map<UUID, BendingUser> entities;

  BenderRegistry(@NonNull BendingStorage storage) {
    cache = Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(2)).buildAsync(storage::createProfile);
    players = new ConcurrentHashMap<>();
    entities = new ConcurrentHashMap<>();
  }

  public boolean isRegistered(@NonNull UUID uuid) {
    return players.containsKey(uuid) || entities.containsKey(uuid);
  }

  /**
   * @param player the bukkit player object
   * @return the BendingPlayer instance associated with the specified player
   */
  public @NonNull BendingPlayer player(@NonNull Player player) {
    return Objects.requireNonNull(players.get(player.getUniqueId()));
  }

  public Optional<BendingUser> user(@NonNull LivingEntity entity) {
    if (entity instanceof Player) {
      return Optional.of(player((Player) entity));
    }
    return Optional.ofNullable(entities.get(entity.getUniqueId()));
  }

  public @NonNull Collection<@NonNull BendingPlayer> onlinePlayers() {
    return players.values().stream().filter(BendingPlayer::valid).collect(Collectors.toList());
  }

  public void invalidateUser(@NonNull UUID uuid) {
    players.remove(uuid);
    entities.remove(uuid);
    cache.synchronous().invalidate(uuid);
  }

  public void createPlayer(@NonNull Player player, @NonNull BendingProfile profile) {
    BendingPlayer.createPlayer(player, profile).ifPresent(user -> {
      players.put(player.getUniqueId(), user);
      Bending.game().abilityManager(user.world()).createPassives(user);
      Bending.game().boardManager().canUseScoreboard(player);
      Bending.eventBus().postBendingPlayerLoadEvent(user);
    });
  }

  public void createUser(@NonNull LivingEntity entity, @NonNull BenderData data) {
    BendingUser.createUser(entity, data).ifPresent(user -> {
      entities.put(entity.getUniqueId(), user);
      Bending.game().abilityManager(user.world()).createPassives(user);
    });
  }

  public @Nullable BendingProfile profile(@NonNull UUID uuid) {
    return cache.synchronous().get(uuid);
  }
}
