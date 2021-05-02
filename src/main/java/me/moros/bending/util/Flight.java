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

package me.moros.bending.util;

import java.util.HashMap;
import java.util.Map;

import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This is a reference counting object that's used to manage a user's flight.
 * Every time a reference is acquired, it should eventually be released.
 * If the reference count drops below 1 then the user will lose flight.
 */
public class Flight {
  private static final Map<User, Flight> instances = new HashMap<>();

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

  // Returns the Flight instance for a user. This will also increment the flight counter.
  // Call release() to decrement the counter.
  // Call remove() to completely remove flight.
  public static @NonNull Flight get(@NonNull User user) {
    return instances.computeIfAbsent(user, Flight::new).increment();
  }

  private Flight increment() {
    ++references;
    return this;
  }

  public void flying(boolean value) {
    isFlying = value;
    user.allowFlight(value);
    user.flying(value);
    changedFlying = true;
  }

  public @NonNull User user() {
    return user;
  }

  // Decrements the user's flight counter. If this goes below 1 then the user loses flight.
  public void release() {
    if (--references < 1) {
      remove(user);
    }
  }

  public static boolean hasFlight(@NonNull User user) {
    return instances.containsKey(user);
  }

  // Completely releases flight for the user.
  // This will set the user back to the state before any Flight was originally added.
  public static void remove(@NonNull User user) {
    revertFlight(instances.remove(user));
  }

  public static void removeAll() {
    instances.values().forEach(Flight::revertFlight);
    instances.clear();
  }

  private static void revertFlight(Flight flight) {
    if (flight != null && flight.changedFlying) {
      flight.user.allowFlight(flight.couldFly);
      flight.user.flying(flight.wasFlying);
    }
  }

  public static void updateAll() {
    for (Map.Entry<User, Flight> entry : instances.entrySet()) {
      User user = entry.getKey();
      Flight flight = entry.getValue();
      if (flight.changedFlying && user.flying() != flight.isFlying) {
        user.flying(flight.isFlying);
      }
    }
  }
}
