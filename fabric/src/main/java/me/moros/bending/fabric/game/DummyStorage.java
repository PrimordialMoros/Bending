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

package me.moros.bending.fabric.game;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.profile.Identifiable;
import me.moros.bending.api.user.profile.PlayerBenderProfile;
import org.checkerframework.checker.nullness.qual.Nullable;

final class DummyStorage implements BendingStorage {
  static final BendingStorage INSTANCE = new DummyStorage();

  private DummyStorage() {
  }

  @Override
  public boolean init() {
    return true; // TODO false?
  }

  @Override
  public PlayerBenderProfile loadOrCreateProfile(UUID uuid) {
    throw new UnsupportedOperationException("Can't create profile in dummy storage");
  }

  @Override
  public CompletableFuture<@Nullable PlayerBenderProfile> loadProfileAsync(UUID uuid) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void saveProfilesAsync(Iterable<PlayerBenderProfile> profiles) {
  }

  @Override
  public CompletableFuture<Integer> savePresetAsync(Identifiable user, Preset preset) {
    return CompletableFuture.completedFuture(0);
  }

  @Override
  public void deletePresetAsync(Identifiable user, Preset preset) {
  }

  @Override
  public void close() {
  }
}
