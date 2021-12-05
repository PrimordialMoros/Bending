/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.moros.bending.Bending;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This is a reference counting object that's used to manage a user's flight.
 * Every time a reference is acquired, it should eventually be released.
 * If the reference count drops below 1 then the user will lose flight.
 */
public final class FlightManager {
  private final Map<UUID, Flight> instances;

  FlightManager() {
    instances = new HashMap<>();
  }

  public boolean hasFlight(@NonNull User user) {
    return instances.containsKey(user.uuid());
  }

  public @NonNull Flight get(@NonNull User user) {
    Flight flight = instances.computeIfAbsent(user.uuid(), u -> new Flight(user));
    flight.references++;
    return flight;
  }

  public void remove(@NonNull User user) {
    Flight instance = instances.remove(user.uuid());
    if (instance != null) {
      instance.revert();
    }
  }

  public void removeAll() {
    instances.values().forEach(Flight::revert);
    instances.clear();
  }

  public void update() {
    instances.values().forEach(Flight::update);
  }

  public static class Flight {
    private final User user;

    private final boolean couldFly;
    private final boolean wasFlying;

    private boolean isFlying = false;
    private boolean changedFlying = false;
    private int references = 0;

    private Flight(User user) {
      this.user = user;
      couldFly = user.allowFlight();
      wasFlying = user.flying();
    }

    public void flying(boolean value) {
      isFlying = value;
      user.allowFlight(value);
      user.flying(value);
      changedFlying = true;
    }

    /**
     * Decrements the user's flight counter. If this goes below 1 then the user loses flight.
     */
    public void release() {
      if (--references < 1) {
        Bending.game().flightManager().remove(user);
      }
    }

    private void revert() {
      if (changedFlying) {
        user.allowFlight(couldFly);
        user.flying(wasFlying);
      }
    }

    private void update() {
      if (changedFlying && user.flying() != isFlying) {
        user.flying(isFlying);
      }
    }
  }
}
