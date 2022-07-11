/*
 * Copyright 2020-2022 Moros
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.Bending;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.manager.AbilityManager;
import me.moros.bending.model.manager.DummyAbilityManager;
import me.moros.bending.model.manager.WorldManager;
import me.moros.bending.model.user.User;
import me.moros.bending.util.TextUtil;
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.slf4j.Logger;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public final class WorldManagerImpl implements WorldManager, Listener {
  private final Logger logger;
  private final Config config;
  private final Map<World, ManagerPair> worlds;

  WorldManagerImpl(Bending plugin) {
    this.logger = plugin.logger();
    worlds = new ConcurrentHashMap<>();
    config = ConfigManager.load(Config::new);
    for (World world : plugin.getServer().getWorlds()) {
      if (config.contains(world)) {
        Metadata.add(world, Metadata.DISABLED);
      } else {
        worlds.put(world, new ManagerPair(logger));
      }
    }
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public AbilityManager instance(World world) {
    return isEnabled(world) ? worlds.computeIfAbsent(world, w -> new ManagerPair(logger)).abilities : DummyAbilityManager.DUMMY;
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
  public void destroyAllInstances() {
    worlds.values().forEach(w -> w.abilities.destroyAllInstances());
  }

  @Override
  public void createPassives(User user) {
    instance(user.world()).createPassives(user);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldLoad(WorldLoadEvent event) {
    World world = event.getWorld();
    if (config.contains(event.getWorld())) {
      Metadata.add(world, Metadata.DISABLED);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onWorldUnload(WorldUnloadEvent event) {
    ManagerPair pair = worlds.remove(event.getWorld());
    if (pair != null) {
      pair.abilities.destroyAllInstances();
    }
  }

  private static final class ManagerPair {
    private final AbilityManager abilities;
    private final CollisionManager collisions;

    private ManagerPair(Logger logger) {
      abilities = new AbilityManagerImpl(logger);
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
    private List<String> disabledWorlds;

    private boolean contains(World world) {
      for (String value : disabledWorlds) {
        if (world.getName().equalsIgnoreCase(value) || world.getUID().equals(TextUtil.parseUUID(value))) {
          return true;
        }
      }
      return false;
    }

    @Override
    public Iterable<String> path() {
      return List.of("properties", "disabled-worlds");
    }
  }
}
