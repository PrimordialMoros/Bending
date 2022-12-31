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

package me.moros.bending.model.ability.common.basic;

import me.moros.bending.model.manager.FlightManager.Flight;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.property.EntityProperty;
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
    user.setProperty(EntityProperty.SPRINTING, sprinting);
    flight.flying(false);
    flight.release();
  }

  void resetSprintAndFall() {
    user.fallDistance(0);
    if (sprinting == TriState.NOT_SET) {
      sprinting = user.checkProperty(EntityProperty.SNEAKING);
    }
    user.setProperty(EntityProperty.SPRINTING, false);
  }
}
