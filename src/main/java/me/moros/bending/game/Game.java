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

import java.util.UUID;
import java.util.stream.Collectors;

import me.moros.atlas.acf.lib.timings.MCTiming;
import me.moros.bending.Bending;
import me.moros.bending.board.BoardManager;
import me.moros.bending.game.manager.PlayerManager;
import me.moros.bending.game.manager.SequenceManager;
import me.moros.bending.game.manager.WorldManager;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempArmorStand;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.AbilityManager;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.protection.ProtectionSystem;
import me.moros.bending.storage.BendingStorage;
import me.moros.bending.util.Flight;
import me.moros.bending.util.MovementHandler;
import me.moros.bending.util.Tasker;
import org.bukkit.World;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This object holds all the needed bending sub-systems.
 * @see Bending#game
 */
public final class Game {
  private final BendingStorage storage;
  private final ProtectionSystem protectionSystem;

  private final AbilityRegistry abilityRegistry;
  private final SequenceManager sequenceManager;
  private final WorldManager worldManager;

  private final AttributeSystem attributeSystem;
  private final ActivationController activationController;
  private final BoardManager boardManager;

  private final BenderRegistry benderRegistry;
  private final PlayerManager playerManager;

  public Game(@NonNull BendingStorage storage) {
    this.storage = storage;
    protectionSystem = new ProtectionSystem();

    abilityRegistry = new AbilityRegistry();
    sequenceManager = new SequenceManager(this);
    worldManager = new WorldManager();

    attributeSystem = new AttributeSystem();
    activationController = new ActivationController(this);
    boardManager = new BoardManager();

    new AbilityInitializer(this);
    loadStorage();

    TempArmor.init();
    TempBlock.init();
    TempArmorStand.init();
    BendingFallingBlock.init();

    benderRegistry = new BenderRegistry();
    playerManager = new PlayerManager(storage);

    playerManager.onlinePlayers().forEach(worldManager::createPassives);

    Tasker.repeatingTask(this::update, 1);
    Tasker.repeatingTask(FireTick::cleanup, 5);
  }

  private void update() {
    MCTiming timing = Bending.timingManager().ofStart("Bending Update");
    activationController.clearCache();
    worldManager.update();
    Flight.updateAll();
    timing.stopTiming();
  }

  public boolean isDisabledWorld(@NonNull UUID worldID) {
    return worldManager.isDisabledWorld(worldID);
  }

  public void reload() {
    worldManager.destroyAllInstances();
    sequenceManager.clear();
    removeTemporary();
    Bending.configManager().reload();
    Bending.translationManager().reload();
    playerManager.onlinePlayers().forEach(worldManager::createPassives);
  }

  public void cleanup() {
    worldManager.destroyAllInstances();
    removeTemporary();
    Flight.removeAll();
    MovementHandler.resetAll();
    playerManager.onlinePlayers().forEach(storage::savePlayerAsync);
    storage.close();
  }

  private void removeTemporary() {
    TempArmor.MANAGER.removeAll();
    TempBlock.MANAGER.removeAll();
    TempArmorStand.MANAGER.removeAll();
    BendingFallingBlock.MANAGER.removeAll();
  }

  private void loadStorage() {
    storage.createElements(Element.all());
    storage.createAbilities(abilityRegistry.abilities().collect(Collectors.toSet()));
  }

  public @NonNull BendingStorage storage() {
    return storage;
  }

  public @NonNull ProtectionSystem protectionSystem() {
    return protectionSystem;
  }

  public @NonNull AbilityRegistry abilityRegistry() {
    return abilityRegistry;
  }

  public @NonNull SequenceManager sequenceManager() {
    return sequenceManager;
  }

  public @NonNull AbilityManager abilityManager(@NonNull World world) {
    return worldManager.instance(world);
  }

  public @NonNull WorldManager worldManager() {
    return worldManager;
  }

  public @NonNull AttributeSystem attributeSystem() {
    return attributeSystem;
  }

  public @NonNull ActivationController activationController() {
    return activationController;
  }

  public @NonNull BoardManager boardManager() {
    return boardManager;
  }

  public @NonNull BenderRegistry benderRegistry() {
    return benderRegistry;
  }

  public @NonNull PlayerManager playerManager() {
    return playerManager;
  }
}
