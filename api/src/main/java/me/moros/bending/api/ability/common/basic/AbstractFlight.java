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

package me.moros.bending.api.ability.common.basic;

import me.moros.bending.api.game.FlightManager.Flight;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.user.User;
import net.kyori.adventure.util.TriState;

abstract class AbstractFlight {
  final User user;
  final Flight flight;
  private TriState sprinting;

  protected AbstractFlight(User user) {
    this.user = user;
    this.sprinting = TriState.NOT_SET;
    this.flight = user.game().flightManager().get(user);
    this.flight.flying(true);
  }

  void cleanup() {
    user.setProperty(EntityProperties.SPRINTING, sprinting);
    flight.flying(false);
    flight.release();
  }

  void resetSprintAndFall() {
    user.setProperty(EntityProperties.FALL_DISTANCE, 0F);
    if (sprinting == TriState.NOT_SET) {
      sprinting = user.checkProperty(EntityProperties.SPRINTING);
    }
    user.setProperty(EntityProperties.SPRINTING, false);
  }
}
