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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.ConfigurationNode;
import me.moros.atlas.configurate.serialize.SerializationException;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.model.DummyAbilityManager;
import me.moros.bending.model.user.BendingPlayer;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class WorldManager {
	private final Map<World, WorldInstance> worlds;
	private final Set<UUID> disabledWorlds;

	public WorldManager() {
		ConfigurationNode node = ConfigManager.getConfig().node("properties", "disabled-worlds");
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

	public @NonNull AbilityManager getInstanceForWorld(@NonNull World world) {
		if (isDisabledWorld(world.getUID())) return DummyAbilityManager.INSTANCE;
		return worlds.computeIfAbsent(world, WorldInstance::new).getAbilityManager();
	}

	public void clearCollisions() {
		worlds.values().forEach(w -> w.getCollisionManager().clear());
	}

	public void update() {
		worlds.values().forEach(w -> w.getAbilityManager().update());
	}

	public void remove(@NonNull World world) {
		worlds.remove(world);
	}

	public void clear() {
		worlds.clear();
	}

	public void destroyAllInstances() {
		worlds.values().forEach(w -> w.getAbilityManager().destroyAllInstances());
	}

	public void createPassives(@NonNull BendingPlayer player) {
		getInstanceForWorld(player.getWorld()).createPassives(player);
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

		private AbilityManager getAbilityManager() {
			return abilities;
		}

		private CollisionManager getCollisionManager() {
			return collisions;
		}
	}
}
