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

package me.moros.bending.game;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import me.moros.bending.BendingPlugin;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.manager.AbilityManager;
import me.moros.bending.model.manager.WorldManager;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public final class WorldManagerImpl implements WorldManager {
  private final Logger logger;
  private final Map<UUID, ManagerPair> worlds;
  private final Set<String> disabledRaw;
  private final Set<UUID> disabled;

  WorldManagerImpl(BendingPlugin plugin, ConfigManager configManager) {
    this.logger = plugin.logger();
    worlds = new ConcurrentHashMap<>();
    disabledRaw = ConcurrentHashMap.newKeySet();
    disabled = ConcurrentHashMap.newKeySet();
    Config config = ConfigManager.load(Config::new);
    var ref = configManager.reference(Config.class, config);
    if (ref != null) {
      ref.subscribe(this::onConfigUpdate);
    }
  }

  private void onConfigUpdate(Config config) {
    disabledRaw.clear();
    disabledRaw.addAll(config.disabledWorlds);
  }

  @Override
  public AbilityManager instance(UUID world) {
    return isEnabled(world) ? computePair(world).abilities : AbilityManager.dummy();
  }

  private ManagerPair computePair(UUID world) {
    return worlds.computeIfAbsent(world, w -> new ManagerPair(logger, world));
  }

  @Override
  public UpdateResult update() {
    worlds.values().forEach(ManagerPair::update);
    return UpdateResult.CONTINUE;
  }

  @Override
  public void clear() {
    worlds.clear();
  }

  @Override
  public boolean isEnabled(UUID world) {
    return !disabled.contains(world);
  }

  @Override
  public void forEach(Consumer<AbilityManager> consumer) {
    for (ManagerPair pair : worlds.values()) {
      consumer.accept(pair.abilities);
    }
  }

  @Override
  public void onWorldLoad(String worldName, UUID world) {
    if (containsLowerCase(worldName) || containsLowerCase(world.toString())) {
      disabled.add(world);
    }
  }

  private boolean containsLowerCase(String value) {
    return disabledRaw.contains(value.toLowerCase(Locale.ROOT));
  }

  @Override
  public void onWorldUnload(UUID world) {
    ManagerPair pair = worlds.remove(world);
    if (pair != null) {
      pair.abilities.destroyAllInstances();
    }
  }

  @Override
  public void onUserChangeWorld(UUID uuid, UUID oldWorld, UUID newWorld) {
    if (isEnabled(newWorld)) {
      User user = Registries.BENDERS.get(uuid);
      if (user != null) {
        user.board().updateAll();
        instance(oldWorld).destroyUserInstances(user);
        instance(newWorld).createPassives(user);
      }
    }
  }

  private static final class ManagerPair {
    private final AbilityManager abilities;
    private final CollisionManager collisions;

    private ManagerPair(Logger logger, UUID world) {
      abilities = new AbilityManagerImpl(logger, world);
      collisions = new CollisionManager(abilities);
    }

    private void update() {
      abilities.update();
      collisions.update();
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Comment("You can specify worlds either by name or UUID")
    private List<String> disabledWorlds = List.of("DisabledBendingWorld");

    private boolean contains(@Nullable String nameOrUuid) {
      if (nameOrUuid != null) {
        for (String value : disabledWorlds) {
          if (nameOrUuid.equalsIgnoreCase(value)) {
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public List<String> path() {
      return List.of("properties", "disabled-worlds");
    }
  }
}
