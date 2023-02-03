/*
 * Copyright 2020-2023 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.api.storage;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.user.profile.PlayerProfile;
import me.moros.storage.Storage;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles all Storage tasks and their concurrency.
 */
public interface BendingStorage extends Storage {
  /**
   * Creates a new profile for the given uuid or returns an existing one if possible.
   */
  PlayerProfile createProfile(UUID uuid);

  /**
   * This method will attempt to load a profile from the database.
   * @param uuid the player's uuid
   * @see #createProfile(UUID)
   */
  CompletableFuture<@Nullable PlayerProfile> loadProfileAsync(UUID uuid);

  /**
   * Asynchronously saves the given profile's data to the database.
   * It updates the stored profile and saves the current elements and bound abilities.
   * @param profile the PlayerProfile to save
   */
  default void saveProfileAsync(PlayerProfile profile) {
    saveProfilesAsync(List.of(profile));
  }

  /**
   * Bulk version of {@link #saveProfileAsync}
   * @param profiles the PlayerProfiles to save
   */
  void saveProfilesAsync(Iterable<PlayerProfile> profiles);

  /**
   * Adds all given abilities to the database
   * @param abilities the abilities to add
   */
  boolean createAbilities(Iterable<AbilityDescription> abilities);

  /**
   * Asynchronously saves the given player's preset to the database.
   * @param playerId the player's profile id
   * @param preset the Preset to save
   */
  CompletableFuture<Boolean> savePresetAsync(int playerId, Preset preset);

  /**
   * Asynchronously deletes the specified preset.
   * @param presetId the id of the preset to delete
   */
  void deletePresetAsync(int presetId);
}
