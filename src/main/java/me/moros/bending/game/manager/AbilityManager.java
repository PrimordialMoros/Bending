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

import me.moros.atlas.acf.lib.timings.MCTiming;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.MultiAbility;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbilityManager {
	private final Map<User, Collection<Ability>> globalInstances = new HashMap<>();
	private final Collection<UserInstance> addQueue = new ArrayList<>();

	protected AbilityManager() {
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
	public void addAbility(@NonNull User user, @NonNull Ability instance) {
		if (instance instanceof MultiAbility) {
			user.addSlotContainer(((MultiAbility) instance).getSlots());
			if (user instanceof BendingPlayer) {
				((BendingPlayer) user).getEntity().getInventory().setHeldItemSlot(0);
			}
		}
		addQueue.add(new UserInstance(user, instance));
	}

	public void changeOwner(@NonNull Ability ability, @NonNull User user) {
		if (ability.getUser().equals(user)) return;
		globalInstances.getOrDefault(ability.getUser(), Collections.emptyList()).remove(ability);
		globalInstances.computeIfAbsent(user, k -> new ArrayList<>()).add(ability);
		ability.setUser(user);
	}

	public void createPassives(@NonNull User user) {
		Collection<AbilityDescription> userPassives = user.getElements().stream()
			.flatMap(Bending.getGame().getAbilityRegistry()::getPassives).collect(Collectors.toList());
		for (AbilityDescription passive : userPassives) {
			destroyInstanceType(user, passive);
			if (user.hasPermission(passive)) {
				Ability ability = passive.createAbility();
				if (ability != null && ability.activate(user, ActivationMethod.PASSIVE)) {
					addAbility(user, ability);
				}
			}
		}
	}

	public void clearPassives(@NonNull User user) {
		getUserInstances(user)
			.filter(a -> a.getDescription().isActivatedBy(ActivationMethod.PASSIVE))
			.forEach(this::destroyAbility);
	}

	public <T extends Ability> boolean hasAbility(@NonNull User user, @NonNull Class<T> type) {
		return getUserInstances(user, type).findAny().isPresent();
	}

	public boolean hasAbility(@NonNull User user, @NonNull AbilityDescription desc) {
		Ability instance = desc.createAbility();
		return instance != null && hasAbility(user, instance.getClass());
	}

	public void destroyInstance(@NonNull User user, @NonNull Ability ability) {
		if (!globalInstances.containsKey(user)) return;
		Collection<Ability> abilities = globalInstances.get(user);
		abilities.remove(ability);
		destroyAbility(ability);
	}

	public boolean destroyInstanceType(@NonNull User user, @NonNull AbilityDescription desc) {
		Ability instance = desc.createAbility();
		return instance != null && destroyInstanceType(user, instance.getClass());
	}

	public <T extends Ability> boolean destroyInstanceType(@NonNull User user, @NonNull Class<T> type) {
		if (!globalInstances.containsKey(user)) return false;
		Collection<Ability> abilities = globalInstances.get(user);
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
		return globalInstances.values().stream().mapToInt(Collection::size).sum();
	}

	private @NonNull Stream<Ability> getQueuedInstances() {
		return addQueue.stream().map(UserInstance::getAbility);
	}

	private @NonNull Stream<Ability> getQueuedUserInstances(@NonNull User user) {
		return addQueue.stream().filter(i -> i.getUser().equals(user)).map(UserInstance::getAbility);
	}

	public @NonNull Stream<Ability> getUserInstances(@NonNull User user) {
		return Stream.concat(getQueuedUserInstances(user), globalInstances.getOrDefault(user, Collections.emptyList()).stream());
	}

	public <T extends Ability> @NonNull Stream<T> getUserInstances(@NonNull User user, @NonNull Class<T> type) {
		return getUserInstances(user).filter(a -> a.getClass() == type).map(type::cast);
	}

	public <T extends Ability> Optional<T> getFirstInstance(@NonNull User user, @NonNull Class<T> type) {
		return getUserInstances(user, type).findFirst();
	}

	public @NonNull Stream<Ability> getInstances() {
		return Stream.concat(getQueuedInstances(), globalInstances.values().stream().flatMap(Collection::stream));
	}

	public <T extends Ability> @NonNull Stream<T> getInstances(@NonNull Class<T> type) {
		return getInstances().filter(a -> a.getClass() == type).map(type::cast);
	}

	// Destroy every instance created by a user.
	// Calls destroy on the ability before removing it.
	public void destroyUserInstances(@NonNull User user) {
		if (!globalInstances.containsKey(user)) return;
		globalInstances.get(user).forEach(this::destroyAbility);
		globalInstances.remove(user);
	}

	// Destroy all instances created by every user.
	// Calls destroy on the ability before removing it.
	public void destroyAllInstances() {
		globalInstances.values().forEach(abilities -> abilities.forEach(this::destroyAbility));
		globalInstances.clear();
	}

	// Updates each ability every tick. Destroys the ability if ability.update() returns UpdateResult.Remove.
	public void update() {
		for (UserInstance i : addQueue) {
			globalInstances.computeIfAbsent(i.getUser(), key -> new ArrayList<>()).add(i.getAbility());
		}
		addQueue.clear();
		Iterator<Map.Entry<User, Collection<Ability>>> globalIterator = globalInstances.entrySet().iterator();
		// Store the removed abilities here so any abilities added during Ability#destroy won't be concurrent.
		Collection<Ability> removed = new ArrayList<>();
		while (globalIterator.hasNext()) {
			Map.Entry<User, Collection<Ability>> entry = globalIterator.next();
			Collection<Ability> instances = entry.getValue();
			Iterator<Ability> iterator = instances.iterator();
			while (iterator.hasNext()) {
				Ability ability = iterator.next();
				UpdateResult result = UpdateResult.REMOVE;
				try (MCTiming timing = Bending.getTimingManager().of(ability.getName()).startTiming()) {
					result = ability.update();
				} catch (Exception e) {
					Bending.getLog().warn(e.getMessage());
				}
				if (result == UpdateResult.REMOVE) {
					removed.add(ability);
					iterator.remove();
				}
			}
			if (entry.getValue().isEmpty()) {
				globalIterator.remove();
			}
		}
		removed.forEach(this::destroyAbility);
	}

	private void destroyAbility(@NonNull Ability ability) {
		ability.onDestroy();
		ability.getUser().removeLastSlotContainer(); // Needed to clean up multi abilities
	}
}
