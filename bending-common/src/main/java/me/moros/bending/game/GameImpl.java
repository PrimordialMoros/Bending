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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import me.moros.bending.BendingPlugin;
import me.moros.bending.config.ConfigProcessor;
import me.moros.bending.event.EventBus;
import me.moros.bending.model.manager.AbilityManager;
import me.moros.bending.model.manager.ActivationController;
import me.moros.bending.model.manager.FlightManager;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.manager.WorldManager;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.registry.Registry;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.temporal.ActionLimiter;
import me.moros.bending.temporal.Cooldown;
import me.moros.bending.temporal.TempArmor;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.temporal.TempLight;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.TextUtil;
import net.kyori.adventure.key.Key;

public final class GameImpl implements Game {
  private final BendingPlugin plugin;
  private final ConfigProcessor configProcessor;
  private final BendingStorage storage;

  private final FlightManager flightManager;
  private final WorldManager worldManager;

  private final ActivationController activationController;

  private final Collection<TemporalManager<?, ?>> temporal;

  public GameImpl(BendingPlugin plugin, BendingStorage storage) {
    this.plugin = plugin;
    this.configProcessor = plugin.configManager().processor();
    this.storage = storage;

    flightManager = new FlightManagerImpl();
    worldManager = new WorldManagerImpl(plugin);

    activationController = new ActivationControllerImpl();
    temporal = initTemporary();

    lockRegistries();
    plugin.configManager().save();
    storage.createAbilities(Registries.ABILITIES);

    Tasker.sync().repeat(this::update, 1);
    Tasker.sync().repeat(BendingEffect::cleanup, 5);

    printInfo();
  }

  private void printInfo() {
    int abilityAmount = Registries.ABILITIES.size();
    int sequenceAmount = Registries.SEQUENCES.size();
    int collisionAmount = Registries.COLLISIONS.size();
    plugin.logger().info(String.format("Found %d registered abilities (%d Sequences)!", abilityAmount, sequenceAmount));
    plugin.logger().info(String.format("Found %d registered collisions!", collisionAmount));
    plugin.logger().info("Registered protection plugins: " + TextUtil.collect(Registries.PROTECTIONS));
    plugin.logger().info("Registered translations: " + TextUtil.collect(plugin.translationManager(), Locale::getLanguage));
  }

  private void lockRegistries() {
    var keys = Registries.keys().toList();
    EventBus.INSTANCE.postRegistryLockEvent(keys);
    keys.stream().map(Registries::get).forEach(Registry::lock);
  }

  private void update() {
    activationController.clearCache();
    try {
      temporal.forEach(TemporalManager::tick);
      worldManager.update();
      flightManager.update();
    } catch (Throwable t) { // The show must go on
      plugin.logger().error(t.getMessage(), t);
    }
  }

  @Override
  public void reload() {
    cleanup(false);
    plugin.translationManager().reload();
    Registries.BENDERS.forEach(u -> worldManager.instance(u.worldKey()).createPassives(u));
  }

  @Override
  public void cleanup(boolean shutdown) {
    worldManager.forEach(AbilityManager::destroyAllInstances);
    flightManager.removeAll();
    temporal.forEach(TemporalManager::removeAll);
    if (shutdown) {
      EventBus.INSTANCE.shutdown();
      plugin.configManager().close();
      storage.saveProfilesAsync(Registries.BENDERS.players().map(BendingPlayer::toProfile).toList());
      Tasker.sync().shutdown();
      Tasker.async().shutdown();
      storage.close();
    }
  }

  private Collection<TemporalManager<?, ?>> initTemporary() {
    return List.of(Cooldown.MANAGER, TempLight.MANAGER, TempEntity.MANAGER,
      ActionLimiter.MANAGER, TempArmor.MANAGER, TempBlock.MANAGER);
  }

  @Override
  public BendingStorage storage() {
    return storage;
  }

  @Override
  public FlightManager flightManager() {
    return flightManager;
  }

  @Override
  public AbilityManager abilityManager(Key world) {
    return worldManager.instance(world);
  }

  @Override
  public WorldManager worldManager() {
    return worldManager;
  }

  @Override
  public ActivationController activationController() {
    return activationController;
  }

  @Override
  public ConfigProcessor configProcessor() {
    return configProcessor;
  }
}
