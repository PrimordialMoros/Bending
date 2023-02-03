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

package me.moros.bending.common.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.moros.bending.api.game.FlightManager;
import me.moros.bending.api.platform.property.EntityProperty;
import me.moros.bending.api.user.User;
import net.kyori.adventure.util.TriState;

public final class FlightManagerImpl implements FlightManager {
  private final Map<UUID, FlightImpl> instances;

  FlightManagerImpl() {
    instances = new HashMap<>();
  }

  @Override
  public boolean hasFlight(User user) {
    return instances.containsKey(user.uuid());
  }

  @Override
  public Flight get(User user) {
    FlightImpl flight = instances.computeIfAbsent(user.uuid(), u -> new FlightImpl(this, user));
    flight.references++;
    return flight;
  }

  @Override
  public void remove(User user) {
    FlightImpl instance = instances.remove(user.uuid());
    if (instance != null) {
      instance.revert();
    }
  }

  @Override
  public void removeAll() {
    instances.values().forEach(FlightImpl::revert);
    instances.clear();
  }

  public UpdateResult update() {
    instances.values().forEach(FlightImpl::update);
    return UpdateResult.CONTINUE;
  }

  public static final class FlightImpl implements Flight {
    private final FlightManager manager;
    private final User user;

    private final TriState couldFly;
    private final TriState wasFlying;
    private TriState isFlying;
    private int references = 0;

    private FlightImpl(FlightManager manager, User user) {
      this.manager = manager;
      this.user = user;
      couldFly = user.checkProperty(EntityProperty.ALLOW_FLIGHT);
      wasFlying = user.checkProperty(EntityProperty.FLYING);
      isFlying = TriState.NOT_SET;
    }

    @Override
    public User user() {
      return user;
    }

    @Override
    public void flying(boolean value) {
      isFlying = TriState.byBoolean(value);
      user.setProperty(EntityProperty.ALLOW_FLIGHT, value);
      user.setProperty(EntityProperty.FLYING, value);
    }

    @Override
    public void release() {
      if (--references < 1) {
        manager.remove(user);
      }
    }

    private void revert() {
      user.setProperty(EntityProperty.ALLOW_FLIGHT, couldFly);
      user.setProperty(EntityProperty.FLYING, wasFlying);
    }

    private void update() {
      if (user.checkProperty(EntityProperty.FLYING) != isFlying) {
        user.setProperty(EntityProperty.FLYING, isFlying);
      }
    }
  }
}
