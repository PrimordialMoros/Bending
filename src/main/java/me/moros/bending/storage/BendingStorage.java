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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.profile.BendingProfile;
import me.moros.storage.Storage;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Handles all Storage tasks and their concurrency
 */
public interface BendingStorage extends Storage {
	BendingProfile createProfile(@NonNull UUID uuid);

	void loadProfileAsync(@NonNull UUID uuid, @NonNull Consumer<BendingProfile> consumer);

	void savePlayerAsync(@NonNull BendingPlayer bendingPlayer);

	boolean createElements(@NonNull Set<@NonNull Element> elements);

	boolean createAbilities(@NonNull Set<@NonNull AbilityDescription> abilities);

	Preset loadPreset(int playerId, @NonNull String name);

	void savePresetAsync(int playerId, @NonNull Preset preset, @NonNull Consumer<Boolean> consumer);

	void deletePresetAsync(int presetId);
}
