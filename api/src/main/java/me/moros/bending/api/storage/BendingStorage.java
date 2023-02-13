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

import java.io.Closeable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.user.profile.Identifiable;
import me.moros.bending.api.user.profile.PlayerBenderProfile;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles all Storage tasks and their concurrency.
 */
public interface BendingStorage extends Closeable {
  void init();

  /**
   * Tries to load the bender profile for the user identified by the given uuid.
   * If the data doesn't exist, it will create a new profile.
   * @param uuid the player's uuid
   * @return the player's bender profile
   */
  PlayerBenderProfile loadOrCreateProfile(UUID uuid);

  /**
   * This method will attempt to load a profile from storage.
   * @param uuid the player's uuid
   * @see #loadOrCreateProfile(UUID)
   */
  CompletableFuture<@Nullable PlayerBenderProfile> loadProfileAsync(UUID uuid);

  /**
   * Asynchronously saves the given profile's data to storage.
   * It updates the stored profile and saves the current elements and bound abilities.
   * @param profile the profile to save
   */
  default void saveProfileAsync(PlayerBenderProfile profile) {
    saveProfilesAsync(List.of(profile));
  }

  /**
   * Bulk version of {@link #saveProfileAsync}
   * @param profiles the profiles to save
   */
  void saveProfilesAsync(Iterable<PlayerBenderProfile> profiles);

  /**
   * Asynchronously saves the given player's preset to storage.
   * @param user the preset owner
   * @param preset the preset to save
   */
  CompletableFuture<Integer> savePresetAsync(Identifiable user, Preset preset);

  /**
   * Asynchronously deletes the specified preset.
   * @param user the preset owner
   * @param preset the preset to delete
   */
  void deletePresetAsync(Identifiable user, Preset preset);

  @Override
  void close();
}
