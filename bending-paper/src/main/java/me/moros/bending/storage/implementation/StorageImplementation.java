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

package me.moros.bending.storage.implementation;

import me.moros.bending.model.user.player.BendingProfile;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.storage.StorageType;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface StorageImplementation {
	StorageType getType();
	void init();
	void close();

	BendingProfile createProfile(UUID uuid);
	Optional<BendingProfile> loadProfile(UUID uuid);
	boolean updateProfile(BendingProfile profile);

	boolean createElements(Set<Element> elements);
	boolean createAbilities(Set<AbilityDescription> abilities);

	boolean saveElements(BendingPlayer bendingPlayer);
	boolean saveSlot(BendingPlayer bendingPlayer, int slotIndex);

	Preset loadPreset(int playerId, String name);
	boolean savePreset(int playerId, Preset preset);
	boolean deletePreset(int presetId);
}
