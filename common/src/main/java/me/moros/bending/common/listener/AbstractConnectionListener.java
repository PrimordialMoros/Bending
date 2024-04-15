/*
 * Copyright 2020-2024 Moros
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.user.profile.BenderProfile;
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
    this.profileCache = Caffeine.newBuilder().maximumSize(64).executor(Executors.newVirtualThreadPerTaskExecutor())
      .expireAfterWrite(Duration.ofMinutes(2)).buildAsync(this::cacheLoad);
  }

  private Game game() {
    return gameSupplier.get();
  }

  private BenderProfile cacheLoad(UUID uuid) {
    BenderProfile profile = game().storage().loadProfile(uuid);
    return profile == null ? BenderProfile.of(uuid) : profile;
  }

  protected CompletableFuture<?> asyncJoin(UUID uuid) {
    // Don't preload data if remote and lazy load is enabled to avoid sync issues in networks
    if (BendingProperties.instance().lazyLoad() && game().storage().isRemote()) {
      return CompletableFuture.completedFuture(null);
    }
    long startTime = System.currentTimeMillis();
    return profileCache.get(uuid).orTimeout(1000, TimeUnit.MILLISECONDS).whenComplete((ignore, t) -> {
      if (t == null) {
        long deltaTime = System.currentTimeMillis() - startTime;
        if (deltaTime > 500) {
          logger.warn("Processing login for %s took %dms.".formatted(uuid, deltaTime));
        }
      } else if (t instanceof TimeoutException) {
        logger.warn("Timed out while retrieving data for " + uuid);
      } else {
        logger.warn(t.getMessage(), t);
      }
    });
  }

  protected void syncJoin(UUID uuid, Supplier<Player> playerSupplier) {
    User.create(game(), playerSupplier.get(), profileCache.get(uuid));
  }

  protected void onQuit(UUID uuid) {
    Registries.BENDERS.getIfExists(uuid).ifPresent(game().activationController()::onUserDeconstruct);
    profileCache.synchronous().invalidate(uuid);
  }
}
