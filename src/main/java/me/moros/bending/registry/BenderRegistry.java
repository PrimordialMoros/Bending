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

package me.moros.bending.registry;

import java.time.Duration;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import me.moros.atlas.caffeine.cache.AsyncLoadingCache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.bending.Bending;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.BendingUser;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.storage.BendingStorage;
import me.moros.bending.util.ExpiringSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Registry for all valid benders.
 */
public final class BenderRegistry implements Registry<User> {
  private AsyncLoadingCache<UUID, PlayerProfile> cache;
  private final ExpiringSet<UUID> recentlyExpiredUsers;
  private final Map<UUID, BendingPlayer> players;
  private final Map<UUID, BendingUser> entities;

  BenderRegistry() {
    recentlyExpiredUsers = new ExpiringSet<>(100);
    players = new ConcurrentHashMap<>();
    entities = new ConcurrentHashMap<>();
  }

  public void init(@NonNull BendingStorage storage) {
    if (cache == null) {
      Objects.requireNonNull(storage);
      cache = Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(2)).buildAsync(storage::createProfile);
    }
  }

  public boolean contains(@NonNull UUID uuid) {
    return players.containsKey(uuid) || entities.containsKey(uuid);
  }

  /**
   * @param player the bukkit player object
   * @return the BendingPlayer instance associated with the specified player
   */
  public @NonNull BendingPlayer user(@NonNull Player player) {
    return Objects.requireNonNull(players.get(player.getUniqueId()));
  }

  public @Nullable BendingUser user(@NonNull LivingEntity entity) {
    // This is needed because player attributes are updated one last time after logout (SPIGOT-924)
    if (recentlyExpiredUsers.contains(entity.getUniqueId())) {
      return null;
    }
    if (entity instanceof Player player) {
      return user(player);
    }
    return entities.get(entity.getUniqueId());
  }

  public @NonNull Collection<@NonNull BendingPlayer> onlinePlayers() {
    return players.values().stream().filter(BendingPlayer::valid).collect(Collectors.toList());
  }

  public void invalidate(@NonNull User user) {
    UUID uuid = user.uuid();
    players.remove(uuid);
    entities.remove(uuid);
    if (cache != null) {
      cache.synchronous().invalidate(uuid);
    }
    recentlyExpiredUsers.add(uuid);
  }

  public void register(@NonNull User user) {
    UUID uuid = user.uuid();
    if (contains(uuid)) {
      return;
    }
    Bending.game().abilityManager(user.world()).createPassives(user);
    if (user instanceof BendingPlayer bendingPlayer) {
      players.put(uuid, bendingPlayer);
      Bending.game().boardManager().tryEnableBoard(bendingPlayer);
      Bending.eventBus().postPlayerLoadEvent(bendingPlayer);
    } else if (user instanceof BendingUser bendingUser) {
      entities.put(uuid, bendingUser);
    }
  }

  public @Nullable PlayerProfile profileSync(@NonNull UUID uuid) {
    return cache == null ? null : cache.synchronous().get(uuid);
  }

  public @NonNull CompletableFuture<@Nullable PlayerProfile> profile(@NonNull UUID uuid) {
    return cache == null ? CompletableFuture.completedFuture(null) : cache.get(uuid);
  }

  @Override
  public @NonNull Iterator<User> iterator() {
    return new UserIterator(players.values().iterator(), entities.values().iterator());
  }

  private record UserIterator(Iterator<BendingPlayer> first, Iterator<BendingUser> second) implements Iterator<User> {
    @Override
    public boolean hasNext() {
      return first.hasNext() || second.hasNext();
    }

    @Override
    public User next() {
      if (first.hasNext()) {
        return first.next();
      }
      return second.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}
