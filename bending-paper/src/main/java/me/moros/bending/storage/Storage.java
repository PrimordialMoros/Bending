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

	public BendingProfile createProfile(UUID uuid) {
		return implementation.createProfile(uuid);
	}

	// Normally create profile handles all loading
	// This is to be used when we want to load a profile that may not exist in the database yet
	public void loadProfileAsync(UUID uuid, Consumer<BendingProfile> consumer) {
		Tasker.newChain().asyncFirst(() -> implementation.loadProfile(uuid)).asyncLast(p -> p.ifPresent(consumer)).execute();
	}

	public void savePlayerAsync(BendingPlayer bendingPlayer) {
		Tasker.newChain().async(() -> {
			implementation.updateProfile(bendingPlayer.getProfile());
			implementation.saveElements(bendingPlayer);
			implementation.saveSlots(bendingPlayer);
		}).execute();
	}

	public void createElements(Set<Element> elements) {
		implementation.createElements(elements);
	}

	public void createAbilities(Set<AbilityDescription> abilities) {
		implementation.createAbilities(abilities);
	}

	//TODO should preset consumer be synced instead?
	public void loadPresetAsync(int playerId, String name, Consumer<Preset> consumer) {
		Tasker.newChain().asyncFirst(() -> implementation.loadPreset(playerId, name))
			.abortIfNull().asyncLast(consumer::accept).execute();
	}

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
