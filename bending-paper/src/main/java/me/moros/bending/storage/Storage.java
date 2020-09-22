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

package me.moros.bending.storage;

import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.user.player.BendingProfile;
import me.moros.bending.model.user.player.PresetHolder;
import me.moros.bending.storage.implementation.StorageImplementation;
import me.moros.bending.util.Tasker;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Handles all Storage tasks and their concurrency
 */
public class Storage {
	private final StorageImplementation implementation;

	public Storage(StorageImplementation implementation) {
		this.implementation = implementation;
		this.implementation.init();
	}

	public void close() {
		implementation.close();
	}

	/**
	 * Creates a new profile for the given uuid or returns an existing one if possible.
	 */
	public BendingProfile createProfile(UUID uuid) {
		return implementation.createProfile(uuid);
	}

	/**
	 * This method will attempt to load a profile from the database and execute the consumer if found.
	 * @param uuid the player's uuid
	 * @param consumer the consumer to executre if a profile was found
	 * @see #createProfile(UUID)
	 */
	public void loadProfileAsync(UUID uuid, Consumer<BendingProfile> consumer) {
		Tasker.newChain().asyncFirst(() -> implementation.loadProfile(uuid)).asyncLast(p -> p.ifPresent(consumer)).execute();
	}

	/**
	 * Asynchronously saves the given bendingPlayer's data to the database.
	 * It updates the profile and stores the current elements and bound abilities.
	 * @param bendingPlayer the BendingPlayer to save
	 */
	public void savePlayerAsync(BendingPlayer bendingPlayer) {
		Tasker.newChain().async(() -> {
			implementation.updateProfile(bendingPlayer.getProfile());
			implementation.saveElements(bendingPlayer);
			implementation.saveSlots(bendingPlayer);
		}).execute();
	}

	/**
	 * Adds all given elements to the database
	 * @param elements the elements to add
	 */
	public void createElements(Set<Element> elements) {
		implementation.createElements(elements);
	}

	/**
	 * Adds all given abilities to the database
	 * @param abilities the abilities to add
	 */
	public void createAbilities(Set<AbilityDescription> abilities) {
		implementation.createAbilities(abilities);
	}

	/**
	 * This is currently loaded asynchronously using a LoadingCache
	 * @see PresetHolder
	 */
	public Preset loadPreset(int playerId, String name) {
		return implementation.loadPreset(playerId, name);
	}

	public void savePresetAsync(int playerId, Preset preset, Consumer<Boolean> consumer) {
		Tasker.newChain().asyncFirst(() -> implementation.savePreset(playerId, preset))
			.abortIfNull().asyncLast(consumer::accept).execute();
	}

	public void deletePresetAsync(int presetId) {
		Tasker.newChain().asyncFirst(() -> implementation.deletePreset(presetId)).execute();
	}
}
