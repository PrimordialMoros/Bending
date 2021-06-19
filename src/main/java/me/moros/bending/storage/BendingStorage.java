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

package me.moros.bending.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.profile.BendingProfile;
import me.moros.storage.Storage;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles all Storage tasks and their concurrency
 */
public interface BendingStorage extends Storage {
  @NonNull BendingProfile createProfile(@NonNull UUID uuid);

  @NonNull CompletableFuture<@Nullable BendingProfile> loadProfileAsync(@NonNull UUID uuid);

  void savePlayerAsync(@NonNull BendingPlayer bendingPlayer);

  boolean createElements(@NonNull Iterable<Element> elements);

  boolean createAbilities(@NonNull Iterable<AbilityDescription> abilities);

  @Nullable Preset loadPreset(int playerId, @NonNull String name);

  @NonNull CompletableFuture<@NonNull Boolean> savePresetAsync(int playerId, @NonNull Preset preset);

  void deletePresetAsync(int presetId);
}
