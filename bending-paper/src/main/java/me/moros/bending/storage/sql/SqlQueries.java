/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.storage.sql;

import me.moros.storage.StorageType;

public enum SqlQueries {
  PLAYER_INSERT("INSERT INTO bending_players (player_uuid) VALUES(?)"),
  PLAYER_SELECT_BY_UUID("SELECT player_id, board FROM bending_players WHERE player_uuid=? LIMIT 1"),
  PLAYER_UPDATE_PROFILE("UPDATE bending_players SET board=? WHERE player_id=?"),

  ABILITIES_SELECT("SELECT ability_id, ability_name FROM bending_abilities"),

  PLAYER_ELEMENTS_SELECT("SELECT element FROM bending_players_elements WHERE player_id=?"),
  PLAYER_ELEMENTS_INSERT("INSERT INTO bending_players_elements (player_id, element) VALUES(?, ?)"),
  PLAYER_ELEMENTS_REMOVE("DELETE FROM bending_players_elements WHERE player_id=?"),

  PLAYER_SLOTS_SELECT("SELECT slot, ability_id FROM bending_players_slots WHERE player_id=?"),
  PLAYER_SLOTS_INSERT("INSERT INTO bending_players_slots (player_id, slot, ability_id) VALUES(?, ?, ?)"),
  PLAYER_SLOTS_REMOVE("DELETE FROM bending_players_slots WHERE player_id=?"),

  PRESET_SELECT("SELECT preset_id, preset_name FROM bending_presets WHERE player_id=?"),
  PRESET_INSERT_NEW("INSERT INTO bending_presets (player_id, preset_name) VALUES(?, ?)"),
  PRESET_REMOVE_FOR_ID("DELETE FROM bending_presets WHERE preset_id=?"),
  PRESET_REMOVE_SPECIFIC("DELETE FROM bending_presets WHERE player_id=? AND preset_name=?"),

  PRESET_SLOTS_SELECT("SELECT slot, ability_id FROM bending_presets_slots WHERE preset_id=?"),
  PRESET_SLOTS_INSERT("INSERT INTO bending_presets_slots (preset_id, slot, ability_id) VALUES(?, ?, ?)");

  private final String query;

  SqlQueries(String query) {
    this.query = query;
  }

  /**
   * @return The SQL query for this enumeration.
   */
  public String query() {
    return query;
  }

  public static String groupInsertAbilities(StorageType type) {
    String column = "ability_name";
    String table = "bending_abilities (" + column + ") ";
    return switch (type) {
      case SQLITE -> "INSERT OR IGNORE INTO " + table + "VALUES(?)";
      case MYSQL, MARIADB, HSQL ->
        "INSERT INTO " + table + "VALUES(?) ON DUPLICATE KEY UPDATE " + column + "=" + column;
      case H2, POSTGRESQL -> "INSERT INTO " + table + "VALUES(?) ON CONFLICT DO NOTHING";
    };
  }
}
