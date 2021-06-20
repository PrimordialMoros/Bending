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
import java.util.Map.Entry;
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
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.storage.BendingStorage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Registry for all valid benders.
 */
public final class BenderRegistry implements Registry<User> {
  private AsyncLoadingCache<UUID, Entry<PlayerProfile, BenderData>> cache;
  private final Map<UUID, BendingPlayer> players;
  private final Map<UUID, BendingUser> entities;

  BenderRegistry() {
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
    if (entity instanceof Player) {
      return user((Player) entity);
    }
    return entities.get(entity.getUniqueId());
  }

  public @NonNull Collection<@NonNull BendingPlayer> onlinePlayers() {
    return players.values().stream().filter(BendingPlayer::valid).collect(Collectors.toList());
  }

  public void invalidate(@NonNull User user) {
    UUID uuid = user.entity().getUniqueId();
    players.remove(uuid);
    entities.remove(uuid);
    if (cache != null) {
      cache.synchronous().invalidate(uuid);
    }
  }

  public void register(@NonNull LivingEntity entity, @NonNull BenderData data) {
    if (contains(entity.getUniqueId())) {
      return;
    }
    BendingUser.createUser(entity, data).ifPresent(user -> {
      entities.put(entity.getUniqueId(), user);
      Bending.game().abilityManager(user.world()).createPassives(user);
    });
  }

  public void register(@NonNull Player player, @NonNull Entry<PlayerProfile, BenderData> data) {
    if (contains(player.getUniqueId())) {
      return;
    }
    BendingPlayer.createUser(player, data.getKey(), data.getValue()).ifPresent(user -> {
      players.put(player.getUniqueId(), user);
      Bending.game().abilityManager(user.world()).createPassives(user);
      Bending.game().boardManager().canUseScoreboard(player);
      Bending.eventBus().postBendingPlayerLoadEvent(user);
    });
  }

  public @Nullable Entry<PlayerProfile, BenderData> profileSync(@NonNull UUID uuid) {
    return cache == null ? null : cache.synchronous().get(uuid);
  }

  public @NonNull CompletableFuture<@Nullable Entry<PlayerProfile, BenderData>> profile(@NonNull UUID uuid) {
    return cache == null ? CompletableFuture.completedFuture(null) : cache.get(uuid);
  }

  @Override
  public @NonNull Iterator<User> iterator() {
    return new UserIterator(players.values().iterator(), entities.values().iterator());
  }

  private static class UserIterator implements Iterator<User> {
    private final Iterator<BendingPlayer> first;
    private final Iterator<BendingUser> second;

    private UserIterator(Iterator<BendingPlayer> first, Iterator<BendingUser> second) {
      this.first = first;
      this.second = second;
    }

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
