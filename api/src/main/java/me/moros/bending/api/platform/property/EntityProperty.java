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

package me.moros.bending.api.platform.property;

public interface EntityProperty {
  BooleanProperty SNEAKING = bool("sneaking");
  BooleanProperty SPRINTING = bool("sprinting");
  BooleanProperty ALLOW_FLIGHT = bool("allowflight");
  BooleanProperty FLYING = bool("flying");
  BooleanProperty GLIDING = bool("gliding");
  BooleanProperty CHARGED = bool("charged");
  BooleanProperty ALLOW_PICKUP = bool("allowpickup");

  private static IntegerProperty integer(String name, int min, int max) {
    return new IntegerProperty(name, min, max);
  }

  private static BooleanProperty bool(String name) {
    return new BooleanProperty(name);
  }
}
