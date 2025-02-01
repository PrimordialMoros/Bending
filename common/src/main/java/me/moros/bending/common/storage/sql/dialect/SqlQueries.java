/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.common.storage.sql.dialect;

sealed interface SqlQueries permits SqlDialect {
  String SELECT_ABILITIES = "SELECT ability_id, ability_name FROM bending_abilities";

  String SELECT_ALL_USER_UUIDS = "SELECT user_id FROM bending_users";
  String SELECT_USER_BY_UUID = "SELECT board FROM bending_users WHERE user_id = ? LIMIT 1";

  String SELECT_USER_ELEMENTS = "SELECT element FROM bending_user_elements WHERE user_id = ?";
  String INSERT_USER_ELEMENTS = "INSERT INTO bending_user_elements (user_id, element) VALUES (?, ?)";
  String REMOVE_USER_ELEMENTS = "DELETE FROM bending_user_elements WHERE user_id = ?";

  String SELECT_USER_PRESETS = "SELECT preset_name, slot, ability_id FROM bending_profiles WHERE user_id = ?";
  String INSERT_USER_PRESET_WITH_ID = "INSERT INTO bending_presets (preset_id, user_id, preset_name) VALUES (?, ?, ?)";
  String REMOVE_USER_PRESET = "DELETE FROM bending_presets WHERE user_id = ? AND preset_name = ?";
  String INSERT_USER_PRESET_SLOTS = "INSERT INTO bending_preset_slots (preset_id, slot, ability_id) VALUES (?, ?, ?)";
}
