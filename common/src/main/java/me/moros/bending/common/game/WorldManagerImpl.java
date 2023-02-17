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

package me.moros.bending.common.game;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.game.AbilityManager;
import me.moros.bending.api.game.WorldManager;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.common.Bending;
import me.moros.bending.common.config.ConfigManager;
import me.moros.bending.common.logging.Logger;
import net.kyori.adventure.key.Key;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public final class WorldManagerImpl implements WorldManager {
  private final Logger logger;
  private final Map<Key, ManagerPair> worlds;
  private final Set<Key> disabled;

  WorldManagerImpl(Bending plugin) {
    this.logger = plugin.logger();
    worlds = new ConcurrentHashMap<>();
    disabled = ConcurrentHashMap.newKeySet();
    refreshDisabled();
  }

  private void refreshDisabled() {
    disabled.clear();
    for (String raw : ConfigManager.load(Config::new).disabledWorlds) {
      Key key = KeyUtil.VANILLA_KEY_MAPPER.apply(raw.toLowerCase(Locale.ROOT));
      if (key != null) {
        disabled.add(key);
      }
    }
  }

  @Override
  public AbilityManager instance(Key world) {
    return isEnabled(world) ? computePair(world).abilities : DummyAbilityManager.INSTANCE;
  }

  private ManagerPair computePair(Key world) {
    return worlds.computeIfAbsent(world, this::createPair);
  }

  private ManagerPair createPair(Key world) {
    AbilityManager abilities = new AbilityManagerImpl(logger, world);
    return new ManagerPair(abilities, new CollisionManager(abilities));
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
  public boolean isEnabled(Key world) {
    return !disabled.contains(world);
  }

  @Override
  public void forEach(Consumer<AbilityManager> consumer) {
    for (ManagerPair pair : worlds.values()) {
      consumer.accept(pair.abilities);
    }
  }

  @Override
  public void onWorldUnload(Key world) {
    ManagerPair pair = worlds.remove(world);
    if (pair != null) {
      pair.abilities.destroyAllInstances();
    }
  }

  @Override
  public void onUserChangeWorld(UUID uuid, Key oldWorld, Key newWorld) {
    if (isEnabled(newWorld)) {
      User user = Registries.BENDERS.get(uuid);
      if (user != null) {
        user.board().updateAll();
        instance(oldWorld).destroyUserInstances(user);
        instance(newWorld).createPassives(user);
      }
    }
  }

  private record ManagerPair(AbilityManager abilities, CollisionManager collisions) {
    private void update() {
      abilities.update();
      collisions.update();
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Comment("You can specify worlds by their name")
    private List<String> disabledWorlds = List.of("DisabledBendingWorld");

    @Override
    public List<String> path() {
      return List.of("properties");
    }
  }
}
