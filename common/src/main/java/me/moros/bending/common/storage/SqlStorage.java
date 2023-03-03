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

package me.moros.bending.common.storage;

import java.nio.ByteBuffer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.user.profile.Identifiable;
import me.moros.bending.api.user.profile.PlayerBenderProfile;
import me.moros.bending.common.Bending;
import me.moros.bending.common.storage.sql.SqlQueries;
import me.moros.storage.SqlStreamReader;
import me.moros.storage.StorageDataSource;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.Batch;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.StatementContext;

final class SqlStorage extends AbstractStorage {
  private final BiMap<AbilityDescription, Integer> abilityMap;

  private final Bending plugin;
  private final StorageDataSource dataSource;
  private final Jdbi DB;

  SqlStorage(Bending plugin, StorageDataSource dataSource) {
    super(plugin.logger());
    this.plugin = plugin;
    this.dataSource = dataSource;
    this.DB = Jdbi.create(this.dataSource.source());
    if (!nativeUuid()) {
      DB.registerArgument(new UUIDArgumentFactory());
    }
    this.abilityMap = HashBiMap.create(64);
  }

  @Override
  public void init() {
    if (!tableExists("bending_players")) {
      String path = "bending/schema/" + dataSource.type().realName() + ".sql";
      Collection<String> statements = SqlStreamReader.parseQueries(plugin.resource(path));
      DB.useHandle(handle -> {
        Batch batch = handle.createBatch();
        statements.forEach(batch::add);
        batch.execute();
      });
    }
    createAbilities();
  }

  @Override
  public void close() {
    dataSource.source().close();
  }

  private void createAbilities() {
    DB.useHandle(handle -> {
      PreparedBatch batch = handle.prepareBatch(SqlQueries.groupInsertAbilities(dataSource.type()));
      for (AbilityDescription desc : Registries.ABILITIES) {
        if (desc.canBind()) {
          batch.bind(0, desc.name()).add();
        }
      }
      batch.execute();
    });
    List<Entry<String, Integer>> col = DB.withHandle(handle ->
      handle.createQuery(SqlQueries.ABILITIES_SELECT.query())
        .map(this::abilityRowMapper).list()
    );
    for (var entry : col) {
      AbilityDescription desc = Registries.ABILITIES.fromString(entry.getKey());
      if (desc != null) {
        abilityMap.forcePut(desc, entry.getValue());
      }
    }
  }

  @Override
  protected int createNewProfileId(UUID uuid) {
    return DB.withHandle(handle -> handle.createUpdate(SqlQueries.PLAYER_INSERT.query()).bind(0, uuid)
      .executeAndReturnGeneratedKeys(("player_id")).mapTo(int.class).one());
  }

  @Override
  protected @Nullable PlayerBenderProfile loadProfile(UUID uuid) {
    Entry<Integer, Boolean> temp = DB.withHandle(handle ->
      handle.createQuery(SqlQueries.PLAYER_SELECT_BY_UUID.query())
        .bind(0, uuid).map(this::profileRowMapper).findOne().orElse(null)
    );
    if (temp != null && temp.getKey() > 0) {
      int id = temp.getKey();
      return BenderProfile.of(id, uuid, temp.getValue(), BenderProfile.of(getSlots(id), getElements(id), getPresets(id)));
    }
    return null;
  }

  @Override
  protected void saveProfile(PlayerBenderProfile profile) {
    saveBoard(profile);
    saveElements(profile);
    saveSlots(profile);
  }

  @Override
  protected int savePreset(Identifiable user, Preset preset) {
    try {
      DB.useHandle(handle ->
        handle.createUpdate(SqlQueries.PRESET_REMOVE_SPECIFIC.query()).bind(0, user.id()).bind(1, preset.name())
      );
    } catch (Exception e) {
      logError(e);
      return 0;
    }
    List<AbilityDescription> abilities = preset.abilities();
    return DB.withHandle(handle -> {
      int presetId = handle.createUpdate(SqlQueries.PRESET_INSERT_NEW.query())
        .bind(0, user.id()).bind(1, preset.name())
        .executeAndReturnGeneratedKeys("preset_id")
        .mapTo(int.class).findOne().orElse(0);
      if (presetId <= 0) {
        return 0;
      }
      PreparedBatch batch = handle.prepareBatch(SqlQueries.PRESET_SLOTS_INSERT.query());
      int size = abilities.size();
      for (int slot = 0; slot < size; slot++) {
        int abilityId = getAbilityId(abilities.get(slot));
        if (abilityId > 0) {
          batch.bind(0, presetId).bind(1, slot + 1).bind(2, abilityId).add();
        }
      }
      batch.execute();
      return presetId;
    });
  }

  @Override
  protected void deletePreset(Identifiable user, Preset preset) {
    DB.useHandle(handle ->
      handle.createUpdate(SqlQueries.PRESET_REMOVE_FOR_ID.query()).bind(0, preset.id()).execute()
    );
  }

  private void saveBoard(PlayerBenderProfile profile) {
    DB.useHandle(handle ->
      handle.createUpdate(SqlQueries.PLAYER_UPDATE_PROFILE.query())
        .bind(0, profile.board()).bind(1, profile.id()).execute()
    );
  }

  private void saveElements(PlayerBenderProfile profile) {
    DB.useHandle(handle -> {
      int id = profile.id();
      handle.createUpdate(SqlQueries.PLAYER_ELEMENTS_REMOVE.query()).bind(0, id).execute();
      PreparedBatch batch = handle.prepareBatch(SqlQueries.PLAYER_ELEMENTS_INSERT.query());
      for (Element element : profile.elements()) {
        batch.bind(0, id).bind(1, element.name().toLowerCase(Locale.ROOT)).add();
      }
      batch.execute();
    });
  }

  private void saveSlots(PlayerBenderProfile profile) {
    DB.useHandle(handle -> {
      int id = profile.id();
      List<AbilityDescription> abilities = profile.slots();
      handle.createUpdate(SqlQueries.PLAYER_SLOTS_REMOVE.query()).bind(0, id).execute();
      PreparedBatch batch = handle.prepareBatch(SqlQueries.PLAYER_SLOTS_INSERT.query());
      int size = abilities.size();
      for (int slot = 0; slot < size; slot++) {
        int abilityId = getAbilityId(abilities.get(slot));
        if (abilityId <= 0) {
          continue;
        }
        batch.bind(0, id).bind(1, slot + 1).bind(2, abilityId).add();
      }
      batch.execute();
    });
  }

  private int getAbilityId(@Nullable AbilityDescription desc) {
    return desc == null ? 0 : abilityMap.getOrDefault(desc, 0);
  }

  private List<AbilityDescription> getSlots(int playerId) {
    return DB.withHandle(handle -> {
      Query query = handle.createQuery(SqlQueries.PLAYER_SLOTS_SELECT.query()).bind(0, playerId);
      return Arrays.asList(slotMapper(query.mapToMap()));
    });
  }

  private Set<Element> getElements(int playerId) {
    return DB.withHandle(handle ->
      handle.createQuery(SqlQueries.PLAYER_ELEMENTS_SELECT.query()).bind(0, playerId)
        .mapTo(String.class).stream().map(Element::fromName).filter(Objects::nonNull).collect(Collectors.toUnmodifiableSet())
    );
  }

  private Set<Preset> getPresets(int playerId) {
    Set<Preset> presets = new HashSet<>();
    return DB.withHandle(handle -> {
      List<Entry<Integer, String>> presetEntries = handle.createQuery(SqlQueries.PRESET_SELECT.query())
        .bind(0, playerId).map(this::presetRowMapper).list();
      for (var entry : presetEntries) {
        int id = entry.getKey();
        String name = entry.getValue();
        if (id > 0) {
          Query query = handle.createQuery(SqlQueries.PRESET_SLOTS_SELECT.query()).bind(0, id);
          AbilityDescription[] abilities = slotMapper(query.mapToMap());
          presets.add(Preset.create(id, name, abilities));
        }
      }
      return presets;
    });
  }

  private AbilityDescription[] slotMapper(Iterable<Map<String, Object>> results) {
    AbilityDescription[] abilities = new AbilityDescription[9];
    for (Map<String, Object> map : results) {
      int slot = (int) map.get("slot");
      int id = (int) map.get("ability_id");
      abilities[slot - 1] = abilityMap.inverse().get(id);
    }
    return abilities;
  }

  private boolean tableExists(String table) {
    try {
      return DB.withHandle(handle -> {
        String catalog = handle.getConnection().getCatalog();
        return handle.queryMetadata(d -> d.getTables(catalog, null, "%", null))
          .map(x -> x.getColumn("TABLE_NAME", String.class)).stream().anyMatch(table::equalsIgnoreCase);
      });
    } catch (Exception e) {
      logError(e);
    }
    return false;
  }

  private Entry<Integer, Boolean> profileRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    return Map.entry(rs.getInt("player_id"), rs.getBoolean("board"));
  }

  private Entry<String, Integer> abilityRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    return Map.entry(rs.getString("ability_name"), rs.getInt("ability_id"));
  }

  private Entry<Integer, String> presetRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    return Map.entry(rs.getInt("preset_id"), rs.getString("preset_name"));
  }

  private boolean nativeUuid() {
    return switch (dataSource.type()) {
      case POSTGRESQL, H2, HSQL -> true;
      default -> false;
    };
  }

  private static final class UUIDArgumentFactory extends AbstractArgumentFactory<UUID> {
    private UUIDArgumentFactory() {
      super(Types.BINARY);
    }

    @Override
    protected Argument build(UUID value, ConfigRegistry config) {
      ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
      buffer.putLong(value.getMostSignificantBits());
      buffer.putLong(value.getLeastSignificantBits());
      return (position, statement, ctx) -> statement.setBytes(position, buffer.array());
    }
  }
}
