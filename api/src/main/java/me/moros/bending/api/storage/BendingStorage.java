/*
 * Copyright 2020-2025 Moros
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

import me.moros.bending.api.user.profile.BenderProfile;
import org.jspecify.annotations.Nullable;

/**
 * Handles all Storage tasks and their concurrency.
 */
public interface BendingStorage {
  /**
   * Load all stored uuids.
   * @return a set of all stored uuids
   */
  Set<UUID> loadUuids();

  /**
   * Asynchronously load all stored uuids.
   * @return a future with a set of all stored uuids
   */
  CompletableFuture<Set<UUID>> loadUuidsAsync();

  /**
   * Attempt to load a stored profile.
   * @param uuid the user's uuid
   * @return the stored profile or null if not found
   */
  @Nullable BenderProfile loadProfile(UUID uuid);

  /**
   * Asynchronously attempt to load a stored profile.
   * @param uuid the user's uuid
   * @return a future with the stored profile
   */
  CompletableFuture<@Nullable BenderProfile> loadProfileAsync(UUID uuid);

  /**
   * Bulk version of {@link #loadProfile(UUID)}.
   * @param uuids the users' uuids
   * @return a map with all matching stored profiles, will not include null values
   */
  Map<UUID, BenderProfile> loadProfiles(Set<UUID> uuids);

  /**
   * Bulk version of {@link #loadProfileAsync(UUID)}.
   * @param uuids the users' uuids
   * @param progressCounter a counter that will be incremented for every processed profile
   * @return a future with the result
   */
  CompletableFuture<Map<UUID, BenderProfile>> loadProfilesAsync(Set<UUID> uuids, LongAdder progressCounter);

  /**
   * Save the given profile.
   * @param profile the profile to save
   * @return true if the profile was successfully saved, false otherwise
   */
  boolean saveProfile(BenderProfile profile);

  /**
   * Asynchronously save the given profile.
   * @param profile the profile to save
   * @return a future with the result
   */
  CompletableFuture<Boolean> saveProfileAsync(BenderProfile profile);

  /**
   * Bulk version of {@link #saveProfile(BenderProfile)}.
   * @param profiles the profiles to save
   * @return true if all profiles were successfully saved, false otherwise
   */
  boolean saveProfiles(Collection<BenderProfile> profiles);

  /**
   * Bulk version of {@link #saveProfileAsync(BenderProfile)}.
   * @param profiles the profiles to save
   * @return a future with the result
   */
  default CompletableFuture<Boolean> saveProfilesAsync(Collection<BenderProfile> profiles) {
    return saveProfilesAsync(profiles, new LongAdder());
  }

  /**
   * Bulk version of {@link #saveProfileAsync(BenderProfile)}.
   * @param profiles the profiles to save
   * @param progressCounter a counter that will be incremented for every processed profile
   * @return a future with the result
   */
  CompletableFuture<Boolean> saveProfilesAsync(Collection<BenderProfile> profiles, LongAdder progressCounter);

  boolean isRemote();

  void close();
}
