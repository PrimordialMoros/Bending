/*
 * Copyright 2020-2026 Moros
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
import java.util.function.Consumer;

import me.moros.bending.api.game.AbilityManager;
import me.moros.bending.api.game.WorldManager;
import me.moros.bending.common.game.DummyAbilityManager;
import net.kyori.adventure.key.Key;

final class DummyWorldManager implements WorldManager {
  static final WorldManager INSTANCE = new DummyWorldManager();

  private DummyWorldManager() {
  }

  @Override
  public UpdateResult update() {
    return UpdateResult.REMOVE;
  }

  @Override
  public AbilityManager instance(Key world) {
    return DummyAbilityManager.INSTANCE;
  }

  @Override
  public boolean isEnabled(Key world) {
    return false;
  }

  @Override
  public void forEach(Consumer<AbilityManager> consumer) {
  }

  @Override
  public void onWorldUnload(Key world) {
  }

  @Override
  public void onUserChangeWorld(UUID uuid, Key oldWorld, Key newWorld) {
  }
}
