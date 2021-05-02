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

package me.moros.bending.game.manager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.moros.atlas.acf.lib.timings.MCTiming;
import me.moros.atlas.configurate.ConfigurationNode;
import me.moros.atlas.configurate.serialize.SerializationException;
import me.moros.bending.Bending;
import me.moros.bending.model.DummyAbilityManager;
import me.moros.bending.model.user.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class WorldManager {
  private final Map<World, WorldInstance> worlds;
  private final Set<UUID> disabledWorlds;

  public WorldManager() {
    ConfigurationNode node = Bending.configManager().config().node("properties", "disabled-worlds");
    List<String> worldNames = Collections.emptyList();
    try {
      worldNames = node.getList(String.class, Collections.emptyList());
    } catch (SerializationException e) {
      // ignore
    }
    disabledWorlds = worldNames.stream().map(Bukkit::getWorld).filter(Objects::nonNull).map(World::getUID)
      .collect(Collectors.toSet());
    worlds = Bukkit.getWorlds().stream().filter(w -> !isDisabledWorld(w.getUID()))
      .collect(Collectors.toConcurrentMap(Function.identity(), WorldInstance::new));
  }

  public @NonNull AbilityManager instance(@NonNull World world) {
    if (isDisabledWorld(world.getUID())) {
      return DummyAbilityManager.INSTANCE;
    }
    return worlds.computeIfAbsent(world, WorldInstance::new).abilities;
  }

  public void update() {
    for (Map.Entry<World, WorldInstance> entry : worlds.entrySet()) {
      MCTiming timing = Bending.timingManager().ofStart(entry.getKey().getName() + ": Bending tick");
      entry.getValue().update();
      timing.stopTiming();
    }
  }

  public void remove(@NonNull World world) {
    worlds.remove(world);
  }

  public void clear() {
    worlds.clear();
  }

  public void destroyAllInstances() {
    worlds.values().forEach(w -> w.abilities.destroyAllInstances());
  }

  public void createPassives(@NonNull BendingPlayer player) {
    instance(player.world()).createPassives(player);
  }

  public boolean isDisabledWorld(@NonNull UUID worldID) {
    return disabledWorlds.contains(worldID);
  }

  private static class WorldInstance {
    private final AbilityManager abilities;
    private final CollisionManager collisions;

    private WorldInstance(World world) {
      abilities = new AbilityManager();
      collisions = new CollisionManager(abilities);
    }

    private void update() {
      abilities.update();
      collisions.update();
    }
  }
}
