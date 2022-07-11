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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import me.moros.bending.Bending;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.event.EventBus;
import me.moros.bending.game.temporal.ActionLimiter;
import me.moros.bending.game.temporal.Cooldown;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempEntity;
import me.moros.bending.game.temporal.TempLight;
import me.moros.bending.model.manager.AbilityManager;
import me.moros.bending.model.manager.ActivationController;
import me.moros.bending.model.manager.FlightManager;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.manager.WorldManager;
import me.moros.bending.model.registry.Registry;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.protection.Protection;
import me.moros.bending.protection.plugin.GriefPreventionProtection;
import me.moros.bending.protection.plugin.LWCProtection;
import me.moros.bending.protection.plugin.TownyProtection;
import me.moros.bending.protection.plugin.WorldGuardProtection;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.Tasker;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

public final class GameImpl implements Game {
  private final Logger logger;
  private final BendingStorage storage;

  private final FlightManager flightManager;
  private final WorldManager worldManager;

  private final ActivationController activationController;

  private final Collection<TemporalManager<?, ?>> temporal;

  public GameImpl(Bending plugin, BendingStorage storage) {
    this.logger = plugin.logger();
    this.storage = storage;

    flightManager = new FlightManagerImpl();
    worldManager = new WorldManagerImpl(plugin);

    activationController = new ActivationControllerImpl(this);
    storage.createAbilities(Registries.ABILITIES);
    registerProtections();

    temporal = initTemporary();
    lockRegistries();

    Tasker.repeat(this::update, 1);
    Tasker.repeat(BendingEffect::cleanup, 5);
  }

  private void registerProtections() {
    tryRegisterProtection("WorldGuard", WorldGuardProtection::new);
    tryRegisterProtection("GriefPrevention", GriefPreventionProtection::new);
    tryRegisterProtection("Towny", TownyProtection::new);
    tryRegisterProtection("LWC", LWCProtection::new);
  }

  private void tryRegisterProtection(String name, Function<Plugin, Protection> factory) {
    if (ConfigManager.config().node("protection", name).getBoolean(true)) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin(name);
      if (plugin != null && plugin.isEnabled()) {
        Protection protection = factory.apply(plugin);
        if (Registries.PROTECTIONS.register(protection)) {
          logger.info("Registered bending protection for " + name);
        } else {
          logger.warn("Unable to register bending protection for " + name);
        }
      }
    }
  }

  private void lockRegistries() {
    var keys = Registries.keys().toList();
    EventBus.INSTANCE.postRegistryLockEvent(keys);
    keys.stream().map(Registries::get).forEach(Registry::lock);
  }

  private void update() {
    activationController.clearCache();
    temporal.forEach(TemporalManager::tick);
    worldManager.update();
    flightManager.update();
  }

  @Override
  public void reload() {
    cleanup(false);
    Registries.BENDERS.forEach(worldManager::createPassives);
  }

  @Override
  public void cleanup(boolean shutdown) {
    worldManager.destroyAllInstances();
    flightManager.removeAll();
    temporal.forEach(TemporalManager::removeAll);
    if (shutdown) {
      Registries.BENDERS.players().forEach(storage::savePlayerAsync);
      Tasker.INSTANCE.shutdown();
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
  public AbilityManager abilityManager(World world) {
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
}
