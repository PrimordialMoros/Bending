/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.bending.Bending;
import me.moros.bending.board.BoardManager;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.game.manager.AbilityManager;
import me.moros.bending.game.manager.PlayerManager;
import me.moros.bending.game.manager.SequenceManager;
import me.moros.bending.game.manager.WorldManager;
import me.moros.bending.game.temporal.BendingFallingBlock;
import me.moros.bending.game.temporal.TempArmor;
import me.moros.bending.game.temporal.TempArmorStand;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.protection.ProtectionSystem;
import me.moros.bending.storage.Storage;
import me.moros.bending.storage.StorageFactory;
import me.moros.bending.util.Flight;
import me.moros.bending.util.Tasker;
import org.bukkit.World;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Game {
	private static PlayerManager playerManager;
	private static ProtectionSystem protectionSystem;

	private static AbilityRegistry abilityRegistry;
	private static WorldManager worldManager;
	private static SequenceManager sequenceManager;
	private static AttributeSystem attributeSystem;
	private static ActivationController activationController;
	private static BoardManager boardManager;

	private static Storage storage;

	public Game() {
		storage = StorageFactory.createInstance();
		worldManager = new WorldManager();
		abilityRegistry = new AbilityRegistry();
		protectionSystem = new ProtectionSystem();
		sequenceManager = new SequenceManager();
		attributeSystem = new AttributeSystem();
		activationController = new ActivationController();

		AbilityInitializer.loadAbilities();
		loadStorage();

		playerManager = new PlayerManager();
		playerManager.getOnlinePlayers().forEach(worldManager::createPassives);
		boardManager = new BoardManager();

		setupTemporary();
		Tasker.createTaskTimer(this::update, 1, 1);
	}

	private void update() {
		worldManager.update();
		updateCooldowns();
		Flight.updateAll();
	}

	private void updateCooldowns() {
		long time = System.currentTimeMillis();
		playerManager.getOnlinePlayers().forEach(bendingPlayer -> {
			Map<AbilityDescription, Long> cooldowns = bendingPlayer.getCooldowns();
			cooldowns.entrySet().stream().filter(e -> time >= e.getValue()).forEach(e -> Bending.getEventBus().postCooldownRemoveEvent(bendingPlayer, e.getKey()));
			cooldowns.entrySet().removeIf(e -> time >= e.getValue());
		});
	}

	public static boolean isDisabledWorld(UUID worldID) {
		return worldManager.isDisabledWorld(worldID);
	}

	public static void reload() {
		worldManager.destroyAllInstances();
		sequenceManager.clear();
		worldManager.clearCollisions();
		removeTemporary();
		ConfigManager.reload();
		playerManager.getOnlinePlayers().forEach(worldManager::createPassives);
	}

	public static void cleanup() {
		worldManager.destroyAllInstances();
		removeTemporary();
		Flight.removeAll();
		playerManager.getOnlinePlayers().forEach(storage::savePlayerAsync);
		storage.close();
	}

	private static void setupTemporary() {
		TempArmor.init();
		TempBlock.init();
		TempArmorStand.init();
		BendingFallingBlock.init();
	}

	private static void removeTemporary() {
		TempArmor.manager.removeAll();
		TempBlock.manager.removeAll();
		TempArmorStand.manager.removeAll();
		BendingFallingBlock.manager.removeAll();
	}

	private static void loadStorage() {
		storage.createElements(Element.getAll());
		storage.createAbilities(Game.getAbilityRegistry().getAbilities().collect(Collectors.toSet()));
	}

	public static void addAbility(User user, Ability instance) {
		worldManager.getInstanceForWorld(user.getWorld()).addAbility(user, instance);
	}

	public static Storage getStorage() {
		return storage;
	}

	public static SequenceManager getSequenceManager() {
		return sequenceManager;
	}

	public static PlayerManager getPlayerManager() {
		return playerManager;
	}

	public static ProtectionSystem getProtectionSystem() {
		return protectionSystem;
	}

	public static AbilityRegistry getAbilityRegistry() {
		return abilityRegistry;
	}

	public static AbilityManager getAbilityManager(World world) {
		return worldManager.getInstanceForWorld(world);
	}

	public static void clearWorld(World world) {
		if (world != null && worldManager != null) worldManager.remove(world);
	}

	public static AttributeSystem getAttributeSystem() {
		return attributeSystem;
	}

	public static ActivationController getActivationController() {
		return activationController;
	}

	public static BoardManager getBoardManager() {
		return boardManager;
	}
}
