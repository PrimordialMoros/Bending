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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import me.moros.bending.Bending;
import me.moros.bending.model.AbilityManager;
import me.moros.bending.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.serialize.SerializationException;

public final class WorldManager {
  public static final AbilityManager DUMMY_INSTANCE = new DummyAbilityManager();

  private final Map<World, ManagerPair> worlds;
  private final Set<UUID> disabledWorlds;

  WorldManager() {
    disabledWorlds = ConcurrentHashMap.newKeySet();
    try {
      for (String w : Bending.configManager().config().node("properties", "disabled-worlds").getList(String.class, List.of())) {
        World world = Bukkit.getWorld(w);
        if (world != null) {
          disabledWorlds.add(world.getUID());
        }
      }
    } catch (SerializationException ignore) {
    }
    worlds = Bukkit.getWorlds().stream().filter(w -> isEnabled(w.getUID()))
      .collect(Collectors.toConcurrentMap(Function.identity(), w -> new ManagerPair()));
  }

  public @NonNull AbilityManager instance(@NonNull World world) {
    return isEnabled(world.getUID()) ? worlds.computeIfAbsent(world, w -> new ManagerPair()).abilities : DUMMY_INSTANCE;
  }

  public void update() {
    for (Map.Entry<World, ManagerPair> entry : worlds.entrySet()) {
      Timing timing = Timings.ofStart(Bending.plugin(), entry.getKey().getName() + " - tick");
      entry.getValue().update();
      timing.stopTiming();
    }
  }

  public void onWorldUnload(@NonNull World world) {
    worlds.remove(world);
  }

  public void clear() {
    worlds.clear();
  }

  public void destroyAllInstances() {
    worlds.values().forEach(w -> w.abilities.destroyAllInstances());
  }

  public void createPassives(@NonNull User user) {
    instance(user.world()).createPassives(user);
  }

  public boolean isEnabled(@NonNull UUID worldID) {
    return !disabledWorlds.contains(worldID);
  }

  private static class ManagerPair {
    private final AbilityManager abilities;
    private final CollisionManager collisions;

    private ManagerPair() {
      abilities = new AbilityManagerImpl();
      collisions = new CollisionManager(abilities);
    }

    private void update() {
      abilities.update();
      collisions.update();
    }
  }

  private static class DummyAbilityManager implements AbilityManager {
    private DummyAbilityManager() {
    }
  }
}
