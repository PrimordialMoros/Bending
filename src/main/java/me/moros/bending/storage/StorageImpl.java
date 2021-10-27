/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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

package me.moros.bending.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.moros.atlas.hikari.HikariDataSource;
import me.moros.atlas.jdbi.v3.core.Jdbi;
import me.moros.atlas.jdbi.v3.core.argument.AbstractArgumentFactory;
import me.moros.atlas.jdbi.v3.core.argument.Argument;
import me.moros.atlas.jdbi.v3.core.config.ConfigRegistry;
import me.moros.atlas.jdbi.v3.core.statement.Batch;
import me.moros.atlas.jdbi.v3.core.statement.PreparedBatch;
import me.moros.atlas.jdbi.v3.core.statement.Query;
import me.moros.atlas.jdbi.v3.core.statement.StatementContext;
import me.moros.bending.Bending;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.registry.Registries;
import me.moros.bending.storage.sql.SqlQueries;
import me.moros.bending.util.Tasker;
import me.moros.storage.SqlStreamReader;
import me.moros.storage.StorageType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;

public final class StorageImpl implements BendingStorage {
  private final HikariDataSource source;
  private final StorageType type;
  private final Logger logger;
  private final Jdbi DB;

  private final BiMap<String, Integer> abilityMap;

  StorageImpl(@NonNull StorageType type, @NonNull Logger logger, @NonNull HikariDataSource source) {
    this.type = type;
    this.logger = logger;
    this.source = source;
    DB = Jdbi.create(this.source);
    if (type != StorageType.H2 && type != StorageType.POSTGRESQL) {
      DB.registerArgument(new UUIDArgumentFactory());
    }
    abilityMap = HashBiMap.create(32);
    if (!tableExists("bending_players")) {
      init();
    }
  }

  private void init() {
    InputStream stream = Objects.requireNonNull(Bending.plugin().getResource(type.schemaPath()), "Null schema.");
    Collection<String> statements = SqlStreamReader.parseQueries(stream);
    DB.useHandle(handle -> {
      Batch batch = handle.createBatch();
      statements.forEach(batch::add);
      batch.execute();
    });
  }

  @Override
  public @NonNull StorageType type() {
    return type;
  }

  @Override
  public void close() {
    source.close();
  }

  /**
   * Creates a new profile for the given uuid or returns an existing one if possible.
   */
  @Override
  public @NonNull PlayerProfile createProfile(@NonNull UUID uuid) {
    PlayerProfile profile = loadProfile(uuid);
    if (profile == null) {
      profile = DB.withHandle(handle -> {
        int id = handle.createUpdate(SqlQueries.PLAYER_INSERT.query()).bind(0, uuid)
          .executeAndReturnGeneratedKeys(("player_id")).mapTo(int.class).one();
        return new PlayerProfile(id);
      });
    }
    return profile;
  }

  /**
   * This method will attempt to load a profile from the database.
   * @param uuid the player's uuid
   * @see #createProfile(UUID)
   */
  @Override
  public @NonNull CompletableFuture<@Nullable PlayerProfile> loadProfileAsync(@NonNull UUID uuid) {
    return Tasker.async(() -> loadProfile(uuid));
  }

  /**
   * Asynchronously saves the given bendingPlayer's data to the database.
   * It updates the profile and stores the current elements and bound abilities.
   * @param bendingPlayer the BendingPlayer to save
   */
  @Override
  public void savePlayerAsync(@NonNull BendingPlayer bendingPlayer) {
    PlayerProfile profileToSave = bendingPlayer.toProfile();
    Tasker.async(() -> {
      updateProfile(profileToSave);
      saveElements(profileToSave);
      saveSlots(profileToSave);
    });
  }

  /**
   * Adds all given abilities to the database
   * @param abilities the abilities to add
   */
  @Override
  public boolean createAbilities(@NonNull Iterable<AbilityDescription> abilities) {
    try {
      DB.useHandle(handle -> {
        PreparedBatch batch = handle.prepareBatch(SqlQueries.groupInsertAbilities(type));
        for (AbilityDescription desc : abilities) {
          batch.bind(0, desc.name()).add();
        }
        batch.execute();
      });
      List<Entry<String, Integer>> col = DB.withHandle(handle ->
        handle.createQuery(SqlQueries.ABILITIES_SELECT.query())
          .map(this::abilityRowMapper).list()
      );
      col.forEach(e -> abilityMap.forcePut(e.getKey().toLowerCase(Locale.ROOT), e.getValue()));
      return true;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return false;
  }

  @Override
  public @NonNull CompletableFuture<Boolean> savePresetAsync(int playerId, @NonNull Preset preset) {
    return Tasker.async(() -> savePreset(playerId, preset));
  }

  @Override
  public void deletePresetAsync(int presetId) {
    Tasker.async(() -> deletePreset(presetId));
  }

  private PlayerProfile loadProfile(UUID uuid) {
    try {
      PlayerProfile temp = DB.withHandle(handle ->
        handle.createQuery(SqlQueries.PLAYER_SELECT_BY_UUID.query())
          .bind(0, uuid).map(this::profileRowMapper).findOne().orElse(null)
      );
      if (temp != null && temp.id() > 0) {
        int id = temp.id();
        BenderData data = new BenderData(getSlots(id), getElements(id), getPresets(id));
        return new PlayerProfile(id, temp.board(), data);
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return null;
  }

  private boolean updateProfile(PlayerProfile profile) {
    try {
      DB.useHandle(handle ->
        handle.createUpdate(SqlQueries.PLAYER_UPDATE_PROFILE.query())
          .bind(0, profile.board()).bind(1, profile.id()).execute()
      );
      return true;
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return false;
  }

  private boolean saveElements(PlayerProfile profile) {
    try {
      DB.useHandle(handle -> {
        int id = profile.id();
        handle.createUpdate(SqlQueries.PLAYER_ELEMENTS_REMOVE.query()).bind(0, id).execute();
        PreparedBatch batch = handle.prepareBatch(SqlQueries.PLAYER_ELEMENTS_INSERT.query());
        for (Element element : profile.benderData().elements()) {
          batch.bind(0, id).bind(1, element.name().toLowerCase(Locale.ROOT)).add();
        }
        batch.execute();
      });
      return true;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return false;
  }

  private boolean saveSlots(PlayerProfile profile) {
    try {
      DB.useHandle(handle -> {
        int id = profile.id();
        List<AbilityDescription> abilities = profile.benderData().slots();
        handle.createUpdate(SqlQueries.PLAYER_SLOTS_REMOVE.query()).bind(0, id).execute();
        PreparedBatch batch = handle.prepareBatch(SqlQueries.PLAYER_SLOTS_INSERT.query());
        for (int slot = 0; slot < abilities.size(); slot++) {
          int abilityId = getAbilityId(abilities.get(slot));
          if (abilityId <= 0) {
            continue;
          }
          batch.bind(0, id).bind(1, slot + 1).bind(2, abilityId).add();
        }
        batch.execute();
      });
      return true;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return false;
  }

  private boolean savePreset(int playerId, Preset preset) {
    if (preset.id() > 0 || !deletePreset(playerId, preset.name())) {
      return false;
    }
    List<AbilityDescription> abilities = preset.abilities();
    try {
      return DB.withHandle(handle -> {
        int presetId = handle.createUpdate(SqlQueries.PRESET_INSERT_NEW.query())
          .bind(0, playerId).bind(1, preset.name())
          .executeAndReturnGeneratedKeys("preset_id")
          .mapTo(int.class).findOne().orElse(0);
        if (presetId <= 0) {
          return false;
        }
        PreparedBatch batch = handle.prepareBatch(SqlQueries.PRESET_SLOTS_INSERT.query());
        for (int slot = 0; slot < abilities.size(); slot++) {
          int abilityId = getAbilityId(abilities.get(slot));
          if (abilityId > 0) {
            batch.bind(0, presetId).bind(1, slot + 1).bind(2, abilityId).add();
          }
        }
        batch.execute();
        return true;
      });
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return false;
  }

  private boolean deletePreset(int presetId) {
    if (presetId <= 0) {
      return false; // It won't exist
    }
    try {
      DB.useHandle(handle ->
        handle.createUpdate(SqlQueries.PRESET_REMOVE_FOR_ID.query()).bind(0, presetId).execute()
      );
      return true;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return false;
  }

  private int getAbilityId(AbilityDescription desc) {
    if (desc == null) {
      return 0;
    }
    return abilityMap.getOrDefault(desc.name().toLowerCase(Locale.ROOT), 0);
  }

  private String getAbilityName(int id) {
    return abilityMap.inverse().get(id);
  }

  private List<AbilityDescription> getSlots(int playerId) {
    try {
      return DB.withHandle(handle -> {
        Query query = handle.createQuery(SqlQueries.PLAYER_SLOTS_SELECT.query()).bind(0, playerId);
        return slotMapper(query.mapToMap());
      });
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return List.of();
  }

  private Set<Element> getElements(int playerId) {
    try {
      return DB.withHandle(handle ->
        handle.createQuery(SqlQueries.PLAYER_ELEMENTS_SELECT.query()).bind(0, playerId)
          .mapTo(String.class).stream().map(Element::fromName).flatMap(Optional::stream).collect(Collectors.toSet())
      );
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return Set.of();
  }

  private Set<Preset> getPresets(int playerId) {
    try {
      Set<Preset> presets = new HashSet<>();
      return DB.withHandle(handle -> {
        List<Entry<Integer, String>> presetEntries = handle.createQuery(SqlQueries.PRESET_SELECT.query())
          .bind(0, playerId).map(this::presetRowMapper).list();
        for (var entry : presetEntries) {
          int id = entry.getKey();
          String name = entry.getValue();
          if (id > 0) {
            Query query = handle.createQuery(SqlQueries.PRESET_SLOTS_SELECT.query()).bind(0, id);
            AbilityDescription[] abilities = slotMapper(query.mapToMap()).toArray(new AbilityDescription[0]);
            presets.add(new Preset(id, name, abilities));
          }
        }
        return presets;
      });
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return Set.of();
  }

  private boolean deletePreset(int playerId, String presetName) {
    try {
      DB.withHandle(handle ->
        handle.createUpdate(SqlQueries.PRESET_REMOVE_SPECIFIC.query()).bind(0, playerId).bind(1, presetName)
      );
      return true;
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return false;
  }

  private List<AbilityDescription> slotMapper(Iterable<Map<String, Object>> results) {
    AbilityDescription[] abilities = new AbilityDescription[9];
    for (Map<String, Object> map : results) {
      int slot = (int) map.get("slot");
      String abilityName = getAbilityName((int) map.get("ability_id"));
      abilities[slot - 1] = Registries.ABILITIES.ability(abilityName);
    }
    return Arrays.asList(abilities);
  }

  private boolean tableExists(String table) {
    try {
      return DB.withHandle(handle -> {
        String catalog = handle.getConnection().getCatalog();
        return handle.queryMetadata(d -> d.getTables(catalog, null, "%", null))
          .map(x -> x.getColumn("TABLE_NAME", String.class)).stream().anyMatch(table::equalsIgnoreCase);
      });
    } catch (Exception e) {
      logger.warn(e.getMessage(), e);
    }
    return false;
  }

  private PlayerProfile profileRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    int id = rs.getInt("player_id");
    return id > 0 ? new PlayerProfile(id, rs.getBoolean("board")) : null;
  }

  private Entry<String, Integer> abilityRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    return Map.entry(rs.getString("ability_name"), rs.getInt("ability_id"));
  }

  private Entry<Integer, String> presetRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    return Map.entry(rs.getInt("preset_id"), rs.getString("preset_name"));
  }

  private static final class UUIDArgumentFactory extends AbstractArgumentFactory<UUID> {
    private UUIDArgumentFactory() {
      super(Types.BINARY);
    }

    @Override
    protected Argument build(UUID value, ConfigRegistry config) {
      ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
      bb.putLong(value.getMostSignificantBits());
      bb.putLong(value.getLeastSignificantBits());
      ByteArrayInputStream stream = new ByteArrayInputStream(bb.array());
      return (position, statement, ctx) -> statement.setBinaryStream(position, stream);
    }
  }
}
