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

import java.util.Collection;
import java.util.List;
import java.util.Locale;

import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.event.EventBus;
import me.moros.bending.api.game.AbilityManager;
import me.moros.bending.api.game.ActivationController;
import me.moros.bending.api.game.FlightManager;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.game.WorldManager;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.registry.Registry;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.temporal.ActionLimiter;
import me.moros.bending.api.temporal.Cooldown;
import me.moros.bending.api.temporal.TempArmor;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempDisplayEntity;
import me.moros.bending.api.temporal.TempEntity;
import me.moros.bending.api.temporal.TempLight;
import me.moros.bending.api.temporal.TemporalManager;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.api.util.TextUtil;
import me.moros.bending.common.Bending;
import me.moros.bending.common.event.EventBusImpl;
import me.moros.bending.common.storage.StorageFactory;

public final class GameImpl implements Game {
  private final Bending plugin;
  private final ConfigProcessor configProcessor;
  private final EventBus eventBus;
  private final FlightManager flightManager;
  private final WorldManager worldManager;
  private final ActivationController activationController;
  private final Collection<TemporalManager<?, ?>> temporal;
  private final BendingStorage storage;

  public GameImpl(Bending plugin) {
    this.plugin = plugin;
    this.configProcessor = plugin.configManager().processor();
    this.eventBus = new EventBusImpl();
    this.flightManager = new FlightManagerImpl();
    this.worldManager = new WorldManagerImpl(plugin.logger());
    this.activationController = new ActivationControllerImpl();
    this.temporal = List.of(Cooldown.MANAGER, TempLight.MANAGER, TempEntity.MANAGER, TempDisplayEntity.MANAGER,
      ActionLimiter.MANAGER, TempArmor.MANAGER, TempBlock.MANAGER);

    lockRegistries();
    this.storage = new StorageFactory(plugin).createInstance();
    plugin.configManager().save();

    Tasker.sync().repeat(this::update, 1);
    Tasker.sync().repeat(BendingEffect::cleanup, 5);

    printInfo();
  }

  private void lockRegistries() {
    var keys = Registries.keys().toList();
    eventBus.postRegistryLockEvent(keys);
    keys.stream().map(Registries::get).forEach(Registry::lock);
  }

  private void printInfo() {
    int abilityAmount = Registries.ABILITIES.size();
    int sequenceAmount = Registries.SEQUENCES.size();
    int collisionAmount = Registries.COLLISIONS.size();
    plugin.logger().info("Found %d registered abilities (%d Sequences)!".formatted(abilityAmount, sequenceAmount));
    plugin.logger().info("Found %d registered collisions!".formatted(collisionAmount));
    plugin.logger().info("Registered protection plugins: " + TextUtil.collect(Registries.PROTECTIONS));
    plugin.logger().info("Registered translations: " + TextUtil.collect(plugin.translationManager(), Locale::getLanguage));
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
    cleanup();
    Registries.BENDERS.forEach(u -> worldManager.instance(u.worldKey()).createPassives(u));
  }

  @Override
  public void cleanup() {
    worldManager.forEach(AbilityManager::destroyAllInstances);
    flightManager.removeAll();
    temporal.forEach(TemporalManager::removeAll);
    var profiles = Registries.BENDERS.players().map(User::toProfile).toList();
    storage.saveProfilesAsync(profiles);
  }

  @Override
  public EventBus eventBus() {
    return eventBus;
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
