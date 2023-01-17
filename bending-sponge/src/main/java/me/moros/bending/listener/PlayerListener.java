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
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.platform.AbilityDamageSource;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.platform.entity.SpongePlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.entity.DestructEntityEvent;
import org.spongepowered.api.event.filter.Getter;
import org.spongepowered.api.event.filter.IsCancelled;
import org.spongepowered.api.event.network.ServerSideConnectionEvent;
import org.spongepowered.api.util.Tristate;
import org.spongepowered.api.world.server.ServerWorld;

public record PlayerListener(Game game, BendingPlugin plugin, AsyncLoadingCache<UUID, PlayerProfile> profileCache) {
  public PlayerListener(Game game, BendingPlugin plugin) {
    this(game, plugin, createCache(game));
  }

  private static AsyncLoadingCache<UUID, PlayerProfile> createCache(Game game) {
    return Caffeine.newBuilder().maximumSize(100).expireAfterWrite(Duration.ofMinutes(2))
      .buildAsync(game.storage()::createProfile);
  }

  private boolean disabledWorld(Entity entity) {
    return !(entity.world() instanceof ServerWorld serverWorld && game.worldManager().isEnabled(PlatformAdapter.fromRsk(serverWorld.key())));
  }

  @Listener(order = Order.EARLY)
  @IsCancelled(Tristate.UNDEFINED)
  public void onPlayerPreLogin(ServerSideConnectionEvent.Auth event) {
    UUID uuid = event.profile().uniqueId();
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

  @Listener(order = Order.EARLY)
  public void onPlayerJoin(ServerSideConnectionEvent.Join event) {
    ServerPlayer player = event.player();
    UUID uuid = player.uniqueId();
    PlayerProfile profile = profileCache.synchronous().get(uuid);
    if (profile != null) {
      User user = BendingPlayer.createUser(game, new SpongePlayer(player), profile).orElse(null);
      if (user != null) {
        Registries.BENDERS.register(user);
        game.abilityManager(user.worldKey()).createPassives(user);
      }
    } else {
      plugin.logger().error("Could not create bending profile for: " + uuid + " (" + player.name() + ")");
    }
  }

  @Listener(order = Order.EARLY)
  public void onPlayerLogout(ServerSideConnectionEvent.Disconnect event) {
    UUID uuid = event.player().uniqueId();
    User user = Registries.BENDERS.get(uuid);
    if (user != null) {
      game.activationController().onUserDeconstruct(user);
    }
    profileCache.synchronous().invalidate(uuid);
  }

  @Listener(order = Order.POST)
  public void onPlayerDeath(DestructEntityEvent.Death event, @Getter("entity") ServerPlayer player) {
    var source = event.cause().first(AbilityDamageSource.class).orElse(null);
    if (source != null) {
      AbilityDescription ability = source.ability();
      TranslatableComponent msg = plugin.translationManager().translate(ability.translationKey() + ".death");
      if (msg == null) {
        msg = Component.translatable("bending.ability.generic.death");
      }
      // TODO check rendering
      event.setMessage(msg.args(player.displayName().get(), source.name(), ability.displayName()));
    }
  }
}
