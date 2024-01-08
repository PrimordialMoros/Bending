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

package me.moros.bending.api.config.attribute;

/**
 * Represents an attribute that can be associated with an ability.
 */
public enum Attribute {
  /**
   * Associated with ability range.
   */
  RANGE("Range"),
  /**
   * Associated with ability selection range.
   */
  SELECTION("Selection"),
  /**
   * Associated with ability cooldown duration.
   */
  COOLDOWN("Cooldown"),
  /**
   * Associated with ability travel speed.
   */
  SPEED("Speed"),
  /**
   * Associated with ability power modifier.
   */
  STRENGTH("Strength"),
  /**
   * Associated with ability damage.
   */
  DAMAGE("Damage"),
  /**
   * Associated with ability charge time duration.
   */
  CHARGE_TIME("ChargeTime"),
  /**
   * Associated with ability duration.
   */
  DURATION("Duration"),
  /**
   * Associated with ability radius.
   */
  RADIUS("Radius"),
  /**
   * Associated with ability height.
   */
  HEIGHT("Height"),
  /**
   * Associated with an amount created by an ability.
   */
  AMOUNT("Amount"),
  /**
   * Associated with ability fire tick duration.
   */
  FIRE_TICKS("FireTicks"),
  /**
   * Associated with ability freeze tick duration.
   */
  FREEZE_TICKS("FreezeTicks");

  private final String value;

  Attribute(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
