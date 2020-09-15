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

package me.moros.bending.game.manager;

import me.moros.bending.config.Configurable;
import me.moros.bending.model.DummyAbilityManager;
import me.moros.bending.model.user.player.BendingPlayer;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WorldManager extends Configurable {
	private static final AbilityManager dummy = new DummyAbilityManager(null);
	private static final Set<UUID> disabledWorlds = new HashSet<>();

	private final Map<World, WorldInstance> worlds = new ConcurrentHashMap<>();

	public WorldManager() {
		for (World w : Bukkit.getWorlds()) {
			if (isDisabledWorld(w.getUID())) continue;
			worlds.put(w, new WorldInstance(w));
		}
	}

	public AbilityManager getInstanceForWorld(World world) {
		if (world == null || isDisabledWorld(world.getUID())) {
			return dummy;
		}
		return worlds.computeIfAbsent(world, WorldInstance::new).getAbilityManager();
	}

	public void clearCollisions() {
		worlds.values().forEach(w -> w.getCollisionManager().clear());
	}

	public void update() {
		worlds.values().forEach(w -> w.getAbilityManager().update());
	}

	public void remove(World world) {
		worlds.remove(world);
	}

	public void clear() {
		worlds.clear();
	}

	public void destroyAllInstances() {
		worlds.values().forEach(w -> w.getAbilityManager().destroyAllInstances());
	}

	public void createPassives(BendingPlayer player) {
		getInstanceForWorld(player.getWorld()).createPassives(player);
	}

	public boolean isDisabledWorld(UUID worldID) {
		return disabledWorlds.contains(worldID);
	}

	private static class WorldInstance {
		private final AbilityManager abilities;
		private final CollisionManager collisions;

		private WorldInstance(World world) {
			abilities = new AbilityManager(world);
			collisions = new CollisionManager(abilities, world);
		}

		private AbilityManager getAbilityManager() {
			return abilities;
		}

		private CollisionManager getCollisionManager() {
			return collisions;
		}
	}

	@Override
	public void onConfigReload() {
		CommentedConfigurationNode node = config.getNode("properties", "disabled-worlds");
		disabledWorlds.clear();
		List<String> temp = node.getList(s -> (String) s, Collections.emptyList());
		for (String name : temp) {
			World w = Bukkit.getWorld(name);
			if (w != null) disabledWorlds.add(w.getUID());
		}
	}
}
