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

import co.aikar.timings.Timing;
import co.aikar.timings.Timings;
import me.moros.bending.Bending;
import me.moros.bending.game.temporal.ActionLimiter;
import me.moros.bending.game.temporal.Cooldown;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempFallingBlock;
import me.moros.bending.game.temporal.TempLight;
import me.moros.bending.game.temporal.TempPacketEntity;
import me.moros.bending.model.manager.AbilityManager;
import me.moros.bending.model.manager.ActivationController;
import me.moros.bending.model.manager.FlightManager;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.manager.WorldManager;
import me.moros.bending.model.storage.BendingStorage;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.Tasker;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class GameImpl implements Game {
  private final BendingStorage storage;

  private final FlightManager flightManager;
  private final SequenceManagerImpl sequenceManager;
  private final WorldManagerImpl worldManager;

  private final ActivationControllerImpl activationController;

  private final Collection<TemporalManager<?, ?>> temporal;

  public GameImpl(@NonNull BendingStorage storage) {
    this.storage = storage;

    flightManager = new FlightManagerImpl();
    sequenceManager = new SequenceManagerImpl();
    worldManager = new WorldManagerImpl();

    activationController = new ActivationControllerImpl(sequenceManager, worldManager);

    new AbilityInitializer();
    storage.createAbilities(Registries.ABILITIES);

    temporal = initTemporary();
    Registries.BENDERS.init(this);

    Tasker.repeat(this::update, 1);
    Tasker.repeat(BendingEffect::cleanup, 5);
  }

  private void update() {
    activationController.clearCache();
    for (var manager : temporal) {
      try (Timing timing = Timings.of(Bending.plugin(), manager.label())) {
        manager.tick();
      } catch (Exception e) {
        Bending.logger().warn(e.getMessage(), e);
      }
    }
    worldManager.update();
    flightManager.update();
  }

  @Override
  public void reload() {
    cleanup(false);
    Bending.configManager().reload();
    Bending.translationManager().reload();
    Registries.BENDERS.forEach(worldManager::createPassives);
  }

  @Override
  public void cleanup(boolean shutdown) {
    worldManager.destroyAllInstances();
    flightManager.removeAll();
    sequenceManager.clear();
    temporal.forEach(TemporalManager::removeAll);

    if (shutdown) {
      Registries.BENDERS.players().forEach(storage::savePlayerAsync);
      Tasker.INSTANCE.shutdown();
      storage.close();
    }
  }

  private Collection<TemporalManager<?, ?>> initTemporary() {
    return List.of(Cooldown.MANAGER, TempLight.MANAGER, TempPacketEntity.MANAGER, ActionLimiter.MANAGER,
      TempArmor.MANAGER, TempFallingBlock.MANAGER, TempBlock.MANAGER);
  }

  @Override
  public @NonNull BendingStorage storage() {
    return storage;
  }

  @Override
  public @NonNull FlightManager flightManager() {
    return flightManager;
  }

  @Override
  public @NonNull AbilityManager abilityManager(@NonNull World world) {
    return worldManager.instance(world);
  }

  @Override
  public @NonNull WorldManager worldManager() {
    return worldManager;
  }

  @Override
  public @NonNull ActivationController activationController() {
    return activationController;
  }
}
