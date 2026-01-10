/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.api.ability.preset;

/**
 * Represents the result of a preset registration query.
 */
public enum PresetRegisterResult {
  /**
   * Preset has been successfully registered.
   */
  SUCCESS,
  /**
   * Preset registration was unable to complete because it already exists.
   */
  EXISTS,
  /**
   * Preset registration was cancelled by an event listener.
   */
  CANCELLED,
  /**
   * Preset registration failed due to a storage error.
   */
  FAIL
}
