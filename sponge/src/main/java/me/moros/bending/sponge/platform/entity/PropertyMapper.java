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

package me.moros.bending.sponge.platform.entity;

import java.util.Map;

import me.moros.bending.api.platform.property.BooleanProperty;
import me.moros.bending.api.platform.property.EntityProperty;
import org.spongepowered.api.data.Key;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.data.value.Value;

import static java.util.Map.entry;

final class PropertyMapper {
  static final Map<BooleanProperty, Key<Value<Boolean>>> PROPERTIES;

  static {
    PROPERTIES = Map.ofEntries(
      entry(EntityProperty.SNEAKING, Keys.IS_SNEAKING),
      entry(EntityProperty.SPRINTING, Keys.IS_SPRINTING),
      entry(EntityProperty.ALLOW_FLIGHT, Keys.CAN_FLY),
      entry(EntityProperty.FLYING, Keys.IS_FLYING),
      entry(EntityProperty.GLIDING, Keys.IS_ELYTRA_FLYING),
      entry(EntityProperty.CHARGED, Keys.IS_CHARGED)
    );
  }
}
