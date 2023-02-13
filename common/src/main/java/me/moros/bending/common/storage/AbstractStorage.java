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

package me.moros.bending.common.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.user.profile.Identifiable;
import me.moros.bending.api.user.profile.PlayerBenderProfile;
import me.moros.bending.api.util.Tasker;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

abstract class AbstractStorage implements BendingStorage {
  protected final Logger logger;

  protected AbstractStorage(Logger logger) {
    this.logger = logger;
  }

  @Override
  public PlayerBenderProfile loadOrCreateProfile(UUID uuid) {
    PlayerBenderProfile profile = loadProfile(uuid);
    if (profile == null) {
      return BenderProfile.of(createNewProfileId(uuid), uuid, true);
    }
    return profile;
  }

  @Override
  public CompletableFuture<@Nullable PlayerBenderProfile> loadProfileAsync(UUID uuid) {
    return Tasker.async().submit(() -> loadProfile(uuid)).exceptionally(this::logError);
  }

  @Override
  public void saveProfilesAsync(Iterable<PlayerBenderProfile> profiles) {
    Tasker.async().submit(() -> profiles.forEach(this::saveProfile)).exceptionally(this::logError);
  }

  @Override
  public CompletableFuture<Integer> savePresetAsync(Identifiable user, Preset preset) {
    if (preset.id() > 0 || preset.name().isEmpty()) {
      return CompletableFuture.completedFuture(0);
    }
    return Tasker.async().submit(() -> savePreset(user, preset)).exceptionally(this::logError0);
  }

  @Override
  public void deletePresetAsync(Identifiable user, Preset preset) {
    if (preset.id() <= 0) {
      return;
    }
    Tasker.async().submit(() -> deletePreset(user, preset)).exceptionally(this::logError);
  }

  protected abstract int createNewProfileId(UUID uuid);

  protected abstract @Nullable PlayerBenderProfile loadProfile(UUID uuid);

  protected abstract void saveProfile(PlayerBenderProfile profile);

  protected abstract int savePreset(Identifiable user, Preset preset);

  protected abstract void deletePreset(Identifiable user, Preset preset);

  protected <R> @Nullable R logError(Throwable t) {
    logger.error(t.getMessage(), t);
    return null;
  }

  protected Integer logError0(Throwable t) {
    logger.error(t.getMessage(), t);
    return 0;
  }
}
