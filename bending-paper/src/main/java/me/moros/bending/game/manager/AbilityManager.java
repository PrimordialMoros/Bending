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

import co.aikar.commands.lib.timings.MCTiming;
import me.moros.bending.Bending;
import me.moros.bending.game.Game;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.MultiAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import me.moros.bending.model.user.player.BendingPlayer;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AbilityManager {
	private final Map<User, List<Ability>> globalInstances = new HashMap<>();
	private final List<UserInstance> addQueue = new ArrayList<>();

	private final World world;

	protected AbilityManager(World world) {
		this.world = world;
	}

	private static class UserInstance {
		private final User user;
		private final Ability ability;

		private UserInstance(User user, Ability ability) {
			this.user = user;
			this.ability = ability;
		}

		private User getUser() {
			return user;
		}

		private Ability getAbility() {
			return ability;
		}
	}

	// Add a new ability instance that should be updated every tick
	// This is deferred until next update to prevent concurrent modifications.
	public void addAbility(User user, Ability instance) {
		if (instance instanceof MultiAbility) {
			user.addSlotContainer(((MultiAbility) instance).getSlots());
			if (user instanceof BendingPlayer) {
				((BendingPlayer) user).getEntity().getInventory().setHeldItemSlot(0);
			}
		}
		addQueue.add(new UserInstance(user, instance));
	}

	public void changeOwner(Ability ability, User user) {
		if (ability.getUser().equals(user)) return;
		List<Ability> previousUserInstances = globalInstances.get(ability.getUser());
		if (previousUserInstances != null) {
			previousUserInstances.remove(ability);
		}
		globalInstances.computeIfAbsent(user, k -> new ArrayList<>()).add(ability);
		ability.setUser(user);
	}

	public void createPassives(User user) {
		for (Element element : user.getElements()) {
			for (AbilityDescription passive : Game.getAbilityRegistry().getPassives(element)) {
				destroyInstanceType(user, passive);
				if (!user.hasPermission("bending.ability." + passive.getName())) continue;
				Ability ability = passive.createAbility();
				if (ability.activate(user, ActivationMethod.PASSIVE)) {
					addAbility(user, ability);
				}
			}
		}
	}

	public void clearPassives(User user) {
		getPlayerInstances(user).stream()
			.filter(a -> a.getDescription().isActivatedBy(ActivationMethod.PASSIVE))
			.forEach(this::destroyAbility);
	}

	public <T extends Ability> boolean hasAbility(User user, Class<T> type) {
		if (!globalInstances.containsKey(user)) return false;
		List<Ability> abilities = globalInstances.get(user);
		if (abilities.stream().anyMatch(a -> a.getClass() == type)) return true;
		return addQueue.stream().anyMatch(i -> i.getUser().equals(user) && i.getAbility().getClass() == type);
	}

	public boolean hasAbility(User user, AbilityDescription desc) {
		return hasAbility(user, desc.createAbility().getClass());
	}

	public void destroyInstance(User user, Ability ability) {
		if (!globalInstances.containsKey(user)) return;
		List<Ability> abilities = globalInstances.get(user);
		abilities.remove(ability);
		destroyAbility(ability);
	}

	public boolean destroyInstanceType(User user, AbilityDescription desc) {
		return destroyInstanceType(user, desc.createAbility().getClass());
	}

	public <T extends Ability> boolean destroyInstanceType(User user, Class<T> type) {
		if (!globalInstances.containsKey(user)) return false;
		List<Ability> abilities = globalInstances.get(user);
		boolean destroyed = false;
		for (Iterator<Ability> iterator = abilities.iterator(); iterator.hasNext(); ) {
			Ability ability = iterator.next();
			if (ability.getClass() == type) {
				iterator.remove();
				destroyAbility(ability);
				destroyed = true;
			}
		}
		return destroyed;
	}

	// Get the number of active abilities.
	public int getInstanceCount() {
		return globalInstances.values().stream().mapToInt(List::size).sum();
	}

	public List<Ability> getPlayerInstances(User user) {
		return globalInstances.getOrDefault(user, Collections.emptyList());
	}

	public <T extends Ability> List<T> getPlayerInstances(User user, Class<T> type) {
		if (!globalInstances.containsKey(user)) return Collections.emptyList();
		List<Ability> abilities = globalInstances.get(user);
		List<T> result = abilities.stream().filter(a -> a.getClass() == type).map(type::cast).collect(Collectors.toList());
		addQueue.stream().filter(i -> i.getUser().equals(user)).map(UserInstance::getAbility)
			.filter(a -> a.getClass() == type).map(type::cast).forEach(result::add);
		return result;
	}

	public <T extends Ability> Optional<T> getFirstInstance(User user, Class<T> type) {
		if (!globalInstances.containsKey(user)) return Optional.empty();
		List<Ability> abilities = globalInstances.get(user);
		Optional<T> result = abilities.stream().filter(a -> a.getClass() == type).map(type::cast).findFirst();
		if (result.isPresent()) return result;
		return addQueue.stream().filter(i -> i.getUser().equals(user)).map(UserInstance::getAbility)
			.filter(a -> a.getClass() == type).map(type::cast).findAny();
	}

	public List<Ability> getInstances() {
		List<Ability> totalInstances = new ArrayList<>(getInstanceCount());
		globalInstances.values().forEach(totalInstances::addAll);
		addQueue.stream().map(UserInstance::getAbility).forEach(totalInstances::add);
		return totalInstances;
	}

	public <T extends Ability> List<T> getInstances(Class<T> type) {
		return getInstances().stream().filter(a -> a.getClass() == type).map(type::cast).collect(Collectors.toList());
	}

	// Destroy every instance created by a player.
	// Calls destroy on the ability before removing it.
	public void destroyPlayerInstances(User user) {
		if (!globalInstances.containsKey(user)) return;
		globalInstances.get(user).forEach(this::destroyAbility);
		globalInstances.remove(user);
	}

	// Destroy all instances created by every player.
	// Calls destroy on the ability before removing it.
	public void destroyAllInstances() {
		globalInstances.values().forEach(abilities -> abilities.forEach(this::destroyAbility));
		globalInstances.clear();
	}

	// Updates each ability every tick. Destroys the ability if ability.update() returns UpdateResult.Remove.
	public void update() {
		addQueue.forEach(i -> globalInstances.computeIfAbsent(i.getUser(), key -> new ArrayList<>()).add(i.getAbility()));
		addQueue.clear();
		Iterator<Map.Entry<User, List<Ability>>> playerIterator = globalInstances.entrySet().iterator();
		// Store the removed abilities here so any abilities added during Ability#destroy won't be concurrent.
		List<Ability> removed = new ArrayList<>();
		while (playerIterator.hasNext()) {
			Map.Entry<User, List<Ability>> entry = playerIterator.next();
			List<Ability> instances = entry.getValue();
			Iterator<Ability> iterator = instances.iterator();
			while (iterator.hasNext()) {
				Ability ability = iterator.next();
				UpdateResult result = UpdateResult.REMOVE;
				try (MCTiming timing = Bending.getTimingManager().of(ability.getName()).startTiming()) {
					result = ability.update();
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (result == UpdateResult.REMOVE) {
					removed.add(ability);
					iterator.remove();
				}
			}
			if (entry.getValue().isEmpty()) {
				playerIterator.remove();
			}
		}
		removed.forEach(this::destroyAbility);
	}

	private void destroyAbility(Ability ability) {
		ability.destroy();
		ability.getUser().removeLastSlotContainer(); // Needed to clean up multi abilities
	}
}
