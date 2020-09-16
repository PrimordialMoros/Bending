/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.storage.sql;

public enum SqlQueries {
	// TODO add support for things other than postgre
	PLAYER_INSERT("INSERT INTO bending_players (uuid) VALUES(?)"),
	PLAYER_SELECT_BY_UUID("SELECT player_id, board FROM bending_players WHERE uuid=? LIMIT 1"),
	PLAYER_UPDATE_BOARD_FOR_ID("UPDATE bending_players SET board=? WHERE player_id=?"),

	ELEMENTS_INSERT_NEW("INSERT INTO bending_elements (element_name) VALUES (?) ON CONFLICT DO NOTHING"),
	ABILITIES_INSERT_NEW("INSERT INTO bending_abilities (ability_name) VALUES (?) ON CONFLICT DO NOTHING"),
	ABILITIES_SELECT_ID_BY_NAME("SELECT ability_id FROM bending_abilities WHERE ability_name=?"),

	PLAYER_ELEMENTS_SELECT_FOR_ID("SELECT e.element_name FROM bending_players_elements pe JOIN bending_elements e ON e.element_id = pe.element_id WHERE pe.player_id=?"),
	PLAYER_ELEMENTS_INSERT_FOR_NAME("INSERT INTO bending_players_elements (element_id, player_id) SELECT e.element_id, ? FROM bending_elements e WHERE e.element_name=?"),
	PLAYER_ELEMENTS_REMOVE_FOR_ID("DELETE FROM bending_players_elements WHERE player_id=?"),

	PLAYER_SLOTS_SELECT_FOR_ID("SELECT ps.slot, a.ability_name FROM bending_players_slots ps JOIN bending_abilities a ON a.ability_id = ps.ability_id WHERE ps.player_id=?"),
	PLAYER_SLOTS_INSERT_NEW("INSERT INTO bending_players_slots (player_id, slot, ability_id) VALUES (?, ?, ?)"),
	PLAYER_SLOTS_REMOVE_SPECIFIC("DELETE FROM bending_players_slots WHERE player_id=? AND slot=?"),

	PRESET_NAMES_SELECT_BY_PLAYER_ID("SELECT preset_name FROM bending_presets WHERE player_id=?"),
	PRESET_SELECT_ID_BY_ID_AND_NAME("SELECT preset_id FROM bending_presets WHERE player_id=? AND preset_name=? LIMIT 1"),
	PRESET_INSERT_NEW("INSERT INTO bending_presets (player_id, preset_name) VALUES(?, ?)"),
	PRESET_REMOVE_FOR_ID("DELETE FROM bending_presets WHERE preset_id=?"),
	PRESET_REMOVE_SPECIFIC("DELETE FROM bending_presets WHERE player_id=? AND preset_name=?"),

	PRESET_SLOTS_SELECT_BY_ID("SELECT ps.slot, a.ability_name FROM bending_presets_slots ps JOIN bending_abilities a ON a.ability_id = ps.ability_id WHERE ps.preset_id=?"),
	PRESET_SLOTS_INSERT_NEW("INSERT INTO bending_presets_slots (preset_id, slot, ability_id) VALUES (?, ?, ?)");

	private final String query;

	SqlQueries(String query) {
		this.query = query;
	}

	/**
	 * Get the SQL query this wraps.
	 *
	 * @return The query for this enumeration.
	 */
	public String getQuery() {
		return query;
	}
}
