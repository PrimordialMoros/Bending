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

import me.moros.atlas.acf.lib.timings.MCTiming;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.board.BoardManager;
import me.moros.bending.game.manager.AbilityManager;
import me.moros.bending.game.manager.PlayerManager;
import me.moros.bending.game.manager.SequenceManager;
import me.moros.bending.game.manager.WorldManager;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempArmorStand;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.Element;
import me.moros.bending.protection.ProtectionSystem;
import me.moros.bending.storage.BendingStorage;
import me.moros.bending.util.Flight;
import me.moros.bending.util.MovementHandler;
import me.moros.bending.util.Tasker;
import org.bukkit.World;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * This object holds all the needed bending sub-systems.
 * @see Bending#getGame
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

		playerManager.getOnlinePlayers().forEach(worldManager::createPassives);

		Tasker.createTaskTimer(this::update, 1, 1);
	}

	private void update() {
		MCTiming timing = Bending.getTimingManager().ofStart("Bending Update");
		activationController.clearSpoutCache();
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
		Bending.getConfigManager().reload();
		Bending.getTranslationManager().reload();
		playerManager.getOnlinePlayers().forEach(worldManager::createPassives);
	}

	public void cleanup() {
		worldManager.destroyAllInstances();
		removeTemporary();
		Flight.removeAll();
		MovementHandler.resetAll();
		playerManager.getOnlinePlayers().forEach(storage::savePlayerAsync);
		storage.close();
	}

	private void removeTemporary() {
		TempArmor.manager.removeAll();
		TempBlock.manager.removeAll();
		TempArmorStand.manager.removeAll();
		BendingFallingBlock.manager.removeAll();
	}

	private void loadStorage() {
		storage.createElements(Element.getAll());
		storage.createAbilities(abilityRegistry.getAbilities().collect(Collectors.toSet()));
	}

	public @NonNull BendingStorage getStorage() {
		return storage;
	}

	public @NonNull ProtectionSystem getProtectionSystem() {
		return protectionSystem;
	}

	public @NonNull AbilityRegistry getAbilityRegistry() {
		return abilityRegistry;
	}

	public @NonNull SequenceManager getSequenceManager() {
		return sequenceManager;
	}

	public @NonNull AbilityManager getAbilityManager(@NonNull World world) {
		return worldManager.getInstanceForWorld(world);
	}

	public void clearWorld(@NonNull World world) {
		worldManager.remove(world);
	}

	public @NonNull AttributeSystem getAttributeSystem() {
		return attributeSystem;
	}

	public @NonNull ActivationController getActivationController() {
		return activationController;
	}

	public @NonNull BoardManager getBoardManager() {
		return boardManager;
	}

	public @NonNull BenderRegistry getBenderRegistry() {
		return benderRegistry;
	}

	public @NonNull PlayerManager getPlayerManager() {
		return playerManager;
	}
}
