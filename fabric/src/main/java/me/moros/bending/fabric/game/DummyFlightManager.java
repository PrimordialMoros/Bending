/*
 * Copyright 2020-2024 Moros
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

import me.moros.bending.api.game.FlightManager;
import me.moros.bending.api.user.User;

final class DummyFlightManager implements FlightManager {
  static final DummyFlightManager INSTANCE = new DummyFlightManager();

  private DummyFlightManager() {
  }

  @Override
  public UpdateResult update() {
    return UpdateResult.REMOVE;
  }

  @Override
  public boolean hasFlight(User user) {
    return false;
  }

  @Override
  public Flight get(User user) {
    return new DummyFlight(user);
  }

  @Override
  public void remove(UUID uuid) {
  }

  @Override
  public void removeAll() {
  }

  private record DummyFlight(User user) implements Flight {
    @Override
    public void flying(boolean value) {
    }

    @Override
    public void release() {
    }
  }
}
