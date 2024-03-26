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

package me.moros.bending.api.platform.item;

import java.util.List;

public enum EquipmentSlot {
  MAINHAND,
  OFFHAND,
  FEET,
  LEGS,
  CHEST,
  HEAD;

  public static final List<EquipmentSlot> HAND = List.of(MAINHAND, OFFHAND);
  public static final List<EquipmentSlot> ARMOR = List.of(FEET, LEGS, CHEST, HEAD);
  public static final List<EquipmentSlot> VALUES = List.of(values());
}
