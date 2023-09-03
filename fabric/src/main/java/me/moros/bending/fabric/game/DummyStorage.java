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

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.profile.BenderProfile;
import org.checkerframework.checker.nullness.qual.Nullable;

final class DummyStorage implements BendingStorage {
  static final BendingStorage INSTANCE = new DummyStorage();

  private DummyStorage() {
  }

  @Override
  public Set<UUID> loadUuids() {
    return Set.of();
  }

  @Override
  public CompletableFuture<Set<UUID>> loadUuidsAsync() {
    return CompletableFuture.completedFuture(Set.of());
  }

  @Override
  public @Nullable BenderProfile loadProfile(UUID uuid) {
    return null;
  }

  @Override
  public CompletableFuture<@Nullable BenderProfile> loadProfileAsync(UUID uuid) {
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public Map<UUID, BenderProfile> loadProfiles(Set<UUID> uuids) {
    return Map.of();
  }

  @Override
  public CompletableFuture<Map<UUID, BenderProfile>> loadProfilesAsync(Set<UUID> uuids, LongAdder progressCounter) {
    return CompletableFuture.completedFuture(Map.of());
  }

  @Override
  public boolean saveProfile(BenderProfile profile) {
    return false;
  }

  @Override
  public CompletableFuture<Boolean> saveProfileAsync(BenderProfile profile) {
    return CompletableFuture.completedFuture(false);
  }

  @Override
  public boolean saveProfiles(Collection<BenderProfile> profiles) {
    return false;
  }

  @Override
  public CompletableFuture<Boolean> saveProfilesAsync(Collection<BenderProfile> profiles) {
    return CompletableFuture.completedFuture(false);
  }

  @Override
  public CompletableFuture<Boolean> saveProfilesAsync(Collection<BenderProfile> profiles, LongAdder progressCounter) {
    return CompletableFuture.completedFuture(false);
  }

  @Override
  public String toString() {
    return "Dummy";
  }

  @Override
  public void close() {
  }
}
