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

package me.moros.bending.common.storage.sql.migration;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.util.UUIDUtil;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public class V3__Migrate_from_legacy extends BaseJavaMigration {
  private final Logger logger;
  private final boolean nativeUuid;

  public V3__Migrate_from_legacy(Logger logger, boolean nativeUuid) {
    this.logger = logger;
    this.nativeUuid = nativeUuid;
  }

  @Override
  public void migrate(Context context) throws Exception {
    Connection conn = context.getConnection();
    if (containsTable(conn, "bending_players")) {
      logger.info("Detected legacy database, attempting to migrate...");
      completeMigration(conn);
    }
  }

  private boolean containsTable(Connection connection, String tableName) throws Exception {
    try (ResultSet rs = connection.getMetaData().getTables(connection.getCatalog(), null, "%", null)) {
      while (rs.next()) {
        if (rs.getString(3).equalsIgnoreCase(tableName)) {
          return true;
        }
      }
    }
    return false;
  }

  private void completeMigration(Connection connection) throws Exception {
    Map<String, UUID> abilities = new HashMap<>(64);
    try (var statement = connection.createStatement()) {
      statement.execute(migratePlayers());
      statement.execute(migratePlayerElements());
      ResultSet rs = statement.executeQuery(selectAbilitiesOld());
      while (rs.next()) {
        abilities.put(rs.getString("ability_name"), UUID.randomUUID());
      }
      if (!abilities.isEmpty()) {
        var prepared = connection.prepareStatement(insertAbilities());
        for (var entry : abilities.entrySet()) {
          bind(prepared, 1, entry.getValue());
          prepared.setString(2, entry.getKey());
          prepared.addBatch();
        }
        prepared.executeBatch();
      }
    }

    Map<UUID, Map<String, String[]>> presets = loadPresets(connection);
    Map<UUID, Map<String, UUID>> presetIds = new HashMap<>(presets.size());
    try (var prepared = connection.prepareStatement(insertPresets())) {
      for (var entry : presets.entrySet()) {
        UUID userId = entry.getKey();
        Map<String, String[]> userPresets = entry.getValue();
        for (String presetName : userPresets.keySet()) {
          UUID presetId = UUID.randomUUID();
          presetIds.computeIfAbsent(userId, k -> new HashMap<>()).put(presetName, presetId);
          bind(prepared, 1, presetId);
          bind(prepared, 2, userId);
          prepared.setString(3, presetName);
          prepared.addBatch();
        }
      }
      prepared.executeBatch();
    }

    try (var prepared = connection.prepareStatement(insertPresetSlots())) {
      for (var entry : presets.entrySet()) {
        UUID userId = entry.getKey();
        Map<String, String[]> userPresets = entry.getValue();
        for (var presetEntry : userPresets.entrySet()) {
          UUID presetId = presetIds.get(userId).get(presetEntry.getKey());
          String[] presetAbilities = presetEntry.getValue();
          for (int i = 0; i < presetAbilities.length; i++) {
            String ability = presetAbilities[i];
            UUID abilityId = ability == null ? null : abilities.get(ability);
            if (abilityId != null) {
              bind(prepared, 1, presetId);
              prepared.setShort(2, (short) (i + 1));
              bind(prepared, 3, abilityId);
              prepared.addBatch();
            }
          }
        }
      }
      prepared.executeBatch();
    }

    try (var statement = connection.createStatement()) {
      statement.execute(cleanupTables());
    }
  }

  private String migratePlayers() {
    return "INSERT INTO bending_users (user_id, board) SELECT player_uuid, board FROM bending_players;";
  }

  private String selectAbilitiesOld() {
    return "SELECT ability_name FROM bending_abilities_old;";
  }

  private String insertAbilities() {
    return "INSERT INTO bending_abilities (ability_id, ability_name) VALUES (?, ?)";
  }

  private String migratePlayerElements() {
    return """
      INSERT INTO bending_user_elements (user_id, element)
      SELECT temp.player_uuid AS user_id, old.element
      FROM bending_players_elements old
      INNER JOIN bending_players temp ON old.player_id = temp.player_id;
      """;
  }

  private Map<UUID, Map<String, String[]>> loadPresets(Connection connection) throws Exception {
    Map<UUID, Map<String, String[]>> result = new HashMap<>(256);
    try (var statement = connection.createStatement()) {
      ResultSet rs = statement.executeQuery(loadPresets());
      while (rs.next()) {
        UUID uuid = mapUuid(rs, "player_uuid");
        String name = rs.getString("preset_name");
        var presetSlots = result.computeIfAbsent(uuid, k -> new HashMap<>()).computeIfAbsent(name, k -> new String[9]);
        int slot = rs.getInt("slot") - 1;
        if (slot >= 0 && slot < presetSlots.length) {
          presetSlots[slot] = rs.getString("ability_name");
        }
      }

      rs = statement.executeQuery(loadSlots());
      while (rs.next()) {
        UUID uuid = mapUuid(rs, "player_uuid");
        var presetSlots = result.computeIfAbsent(uuid, k -> new HashMap<>()).computeIfAbsent("", k -> new String[9]);
        int slot = rs.getInt("slot") - 1;
        if (slot >= 0 && slot < presetSlots.length) {
          presetSlots[slot] = rs.getString("ability_name");
        }
      }
    }
    return result;
  }

  private String loadPresets() {
    return """
      SELECT old.player_uuid, presets.preset_name, preset_slots.slot, abilities.ability_name
      FROM bending_players old
      INNER JOIN bending_presets_old presets ON old.player_id = presets.player_id
      INNER JOIN bending_presets_slots preset_slots ON presets.preset_id = preset_slots.preset_id
      INNER JOIN bending_abilities_old abilities ON preset_slots.ability_id = abilities.ability_id;
      """;
  }

  private String loadSlots() {
    return """
      SELECT old.player_uuid, slots.slot, abilities.ability_name
      FROM bending_players old
      INNER JOIN bending_players_slots slots ON old.player_id = slots.player_id
      INNER JOIN bending_abilities_old abilities ON slots.ability_id = abilities.ability_id;
      """;
  }

  private String insertPresets() {
    return "INSERT INTO bending_presets (preset_id, user_id, preset_name) VALUES (?, ?, ?)";
  }

  private String insertPresetSlots() {
    return "INSERT INTO bending_preset_slots (preset_id, slot, ability_id) VALUES (?, ?, ?)";
  }

  private String cleanupTables() {
    return "DROP TABLE bending_players_elements, bending_presets_slots, bending_presets_old, bending_players_slots, bending_abilities_old, bending_players CASCADE;";
  }

  private void bind(PreparedStatement prepared, int idx, UUID uuid) throws SQLException {
    if (nativeUuid) {
      prepared.setObject(idx, uuid);
    } else {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
      buffer.putLong(uuid.getMostSignificantBits());
      buffer.putLong(uuid.getLeastSignificantBits());
      prepared.setBytes(idx, buffer.array());
    }
  }

  private UUID mapUuid(ResultSet rs, String column) throws SQLException {
    return nativeUuid ? rs.getObject(column, UUID.class) : UUIDUtil.fromBytes(rs.getBytes(column));
  }
}
