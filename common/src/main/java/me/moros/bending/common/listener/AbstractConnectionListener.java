/*
 * Copyright 2020-2023 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.common.listener;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.common.logging.Logger;

public abstract class AbstractConnectionListener {
  private final Logger logger;
  protected final Supplier<Game> gameSupplier;
  protected final AsyncLoadingCache<UUID, BenderProfile> profileCache;

  protected AbstractConnectionListener(Logger logger, Game game) {
    this(logger, Suppliers.cached(game));
  }

  protected AbstractConnectionListener(Logger logger, Supplier<Game> gameSupplier) {
    this.logger = logger;
    this.gameSupplier = gameSupplier;
    this.profileCache = Caffeine.newBuilder().maximumSize(64).executor(Tasker.async())
      .expireAfterWrite(Duration.ofMinutes(2)).buildAsync(this::cacheLoad);
  }

  private Game game() {
    return gameSupplier.get();
  }

  private BenderProfile cacheLoad(UUID uuid) {
    BenderProfile profile = game().storage().loadProfile(uuid);
    return profile == null ? BenderProfile.of(uuid) : profile;
  }

  protected CompletableFuture<BenderProfile> asyncJoin(UUID uuid) {
    long startTime = System.currentTimeMillis();
    return profileCache.get(uuid).orTimeout(1000, TimeUnit.MILLISECONDS).thenApply(profile -> {
      long deltaTime = System.currentTimeMillis() - startTime;
      if (profile != null && deltaTime > 500) {
        logger.warn("Processing login for %s took %dms.".formatted(uuid, deltaTime));
      }
      return profile;
    }).exceptionally(t -> {
      if (t instanceof TimeoutException) {
        logger.warn("Timed out while retrieving data for " + uuid);
      } else {
        logger.warn(t.getMessage(), t);
      }
      return null;
    });
  }

  protected void syncJoin(UUID uuid, Supplier<Player> playerSupplier) {
    BenderProfile profile = profileCache.synchronous().get(uuid);
    User user = User.create(game(), playerSupplier.get(), profile).orElse(null);
    if (user != null) {
      Registries.BENDERS.register(user);
      game().abilityManager(user.worldKey()).createPassives(user);
    }
  }

  protected void onQuit(UUID uuid) {
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      game().activationController().onUserDeconstruct(user);
    }
    profileCache.synchronous().invalidate(uuid);
  }
}
