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

package me.moros.bending.listener;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.benmanes.caffeine.cache.AsyncLoadingCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.moros.bending.BendingPlugin;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.platform.entity.BukkitPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public record ConnectionListener(Game game, BendingPlugin plugin,
                                 AsyncLoadingCache<UUID, PlayerProfile> profileCache) implements Listener {
  public ConnectionListener(Game game, BendingPlugin plugin) {
    this(game, plugin, createCache(game));
  }

  private static AsyncLoadingCache<UUID, PlayerProfile> createCache(Game game) {
    return Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(2))
      .buildAsync(game.storage()::createProfile);
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
    UUID uuid = event.getUniqueId();
    long startTime = System.currentTimeMillis();
    try {
      // Timeout after 1000ms to not block the login thread excessively
      PlayerProfile profile = profileCache.get(uuid).get(1000, TimeUnit.MILLISECONDS);
      long deltaTime = System.currentTimeMillis() - startTime;
      if (profile != null && deltaTime > 500) {
        plugin.logger().warn("Processing login for " + uuid + " took " + deltaTime + "ms.");
      }
    } catch (TimeoutException e) {
      plugin.logger().warn("Timed out while retrieving data for " + uuid);
    } catch (CancellationException | ExecutionException | InterruptedException e) {
      plugin.logger().warn(e.getMessage(), e);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID uuid = player.getUniqueId();
    PlayerProfile profile = profileCache.synchronous().get(uuid);
    if (profile != null) {
      User user = BendingPlayer.createUser(game, new BukkitPlayer(player), profile).orElse(null);
      if (user != null) {
        Registries.BENDERS.register(user);
        game.abilityManager(user.worldKey()).createPassives(user);
      }
    } else {
      plugin.logger().error("Could not create bending profile for: " + uuid + " (" + player.getName() + ")");
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerLogout(PlayerQuitEvent event) {
    UUID uuid = event.getPlayer().getUniqueId();
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      game.activationController().onUserDeconstruct(user);
    }
    profileCache.synchronous().invalidate(uuid);
  }
}
