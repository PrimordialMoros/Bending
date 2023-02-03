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

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.api.user.profile.BenderData;
import me.moros.bending.api.user.profile.PlayerProfile;
import me.moros.bending.api.util.Tasker;
import me.moros.storage.SqlStreamReader;
import me.moros.storage.StorageDataSource;
import me.moros.storage.StorageType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.statement.Batch;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.StatementContext;

public final class StorageImpl implements BendingStorage {
  private final BiMap<AbilityDescription, Integer> abilityMap;

  private final StorageDataSource dataSource;
  private final Jdbi DB;

  StorageImpl(StorageDataSource dataSource) {
    this.dataSource = dataSource;
    this.DB = Jdbi.create(this.dataSource.source());
    if (!nativeUuid()) {
      DB.registerArgument(new UUIDArgumentFactory());
    }
    this.abilityMap = HashBiMap.create(64);
  }

  boolean init(Function<String, InputStream> resourceProvider) {
    if (!tableExists("bending_players")) {
      Collection<String> statements;
      String path = Path.of("schema", dataSource.type().realName() + ".sql").toString();
      try (InputStream stream = resourceProvider.apply(path)) {
        statements = SqlStreamReader.parseQueries(stream);
      } catch (Exception e) {
        dataSource.logger().error(e.getMessage(), e);
        return false;
      }
      DB.useHandle(handle -> {
        Batch batch = handle.createBatch();
        statements.forEach(batch::add);
        batch.execute();
      });
    }
    return true;
  }

  @Override
  public StorageType type() {
    return dataSource.type();
  }

  @Override
  public void close() {
    dataSource.source().close();
  }

  @Override
  public PlayerProfile createProfile(UUID uuid) {
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

  @Override
  public CompletableFuture<@Nullable PlayerProfile> loadProfileAsync(UUID uuid) {
    return Tasker.async().submit(() -> loadProfile(uuid)).exceptionally(t -> {
      dataSource.logger().error(t.getMessage(), t);
      return null;
    });
  }

  @Override
  public void saveProfilesAsync(Iterable<PlayerProfile> profiles) {
    Tasker.async().submit(() -> {
      for (var profileToSave : profiles) {
        updateProfile(profileToSave);
        saveElements(profileToSave);
        saveSlots(profileToSave);
      }
    }).exceptionally(this::logError);
  }

  @Override
  public boolean createAbilities(Iterable<AbilityDescription> abilities) {
    DB.useHandle(handle -> {
      PreparedBatch batch = handle.prepareBatch(SqlQueries.groupInsertAbilities(type()));
      for (AbilityDescription desc : abilities) {
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
    return true;
  }

  @Override
  public CompletableFuture<Boolean> savePresetAsync(int playerId, Preset preset) {
    return Tasker.async().submit(() -> savePreset(playerId, preset)).exceptionally(t -> {
      dataSource.logger().error(t.getMessage(), t);
      return false;
    });
  }

  @Override
  public void deletePresetAsync(int presetId) {
    Tasker.async().submit(() -> deletePresetExact(presetId)).exceptionally(this::logError);
  }

  private @Nullable PlayerProfile loadProfile(UUID uuid) {
    PlayerProfile temp = DB.withHandle(handle ->
      handle.createQuery(SqlQueries.PLAYER_SELECT_BY_UUID.query())
        .bind(0, uuid).map(this::profileRowMapper).findOne().orElse(null)
    );
    if (temp != null && temp.id() > 0) {
      int id = temp.id();
      BenderData data = new BenderData(getSlots(id), getElements(id), getPresets(id));
      return new PlayerProfile(id, temp.board(), data);
    }
    return null;
  }

  private void updateProfile(PlayerProfile profile) {
    DB.useHandle(handle ->
      handle.createUpdate(SqlQueries.PLAYER_UPDATE_PROFILE.query())
        .bind(0, profile.board()).bind(1, profile.id()).execute()
    );
  }

  private void saveElements(PlayerProfile profile) {
    DB.useHandle(handle -> {
      int id = profile.id();
      handle.createUpdate(SqlQueries.PLAYER_ELEMENTS_REMOVE.query()).bind(0, id).execute();
      PreparedBatch batch = handle.prepareBatch(SqlQueries.PLAYER_ELEMENTS_INSERT.query());
      for (Element element : profile.benderData().elements()) {
        batch.bind(0, id).bind(1, element.name().toLowerCase(Locale.ROOT)).add();
      }
      batch.execute();
    });
  }

  private void saveSlots(PlayerProfile profile) {
    DB.useHandle(handle -> {
      int id = profile.id();
      List<AbilityDescription> abilities = profile.benderData().slots();
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

  private boolean savePreset(int playerId, Preset preset) {
    if (preset.id() > 0 || !deletePreset(playerId, preset.name())) {
      return false;
    }
    List<AbilityDescription> abilities = preset.abilities();
    return DB.withHandle(handle -> {
      int presetId = handle.createUpdate(SqlQueries.PRESET_INSERT_NEW.query())
        .bind(0, playerId).bind(1, preset.name())
        .executeAndReturnGeneratedKeys("preset_id")
        .mapTo(int.class).findOne().orElse(0);
      if (presetId <= 0) {
        return false;
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
      return true;
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
          presets.add(new Preset(id, name, abilities));
        }
      }
      return presets;
    });
  }

  private boolean deletePreset(int playerId, String presetName) {
    DB.useHandle(handle ->
      handle.createUpdate(SqlQueries.PRESET_REMOVE_SPECIFIC.query()).bind(0, playerId).bind(1, presetName)
    );
    return true;
  }

  private void deletePresetExact(int presetId) {
    if (presetId > 0) {
      DB.useHandle(handle ->
        handle.createUpdate(SqlQueries.PRESET_REMOVE_FOR_ID.query()).bind(0, presetId).execute()
      );
    }
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
      dataSource.logger().warn(e.getMessage(), e);
    }
    return false;
  }

  private <R> R logError(Throwable t) {
    dataSource.logger().error(t.getMessage(), t);
    return null;
  }

  private @Nullable PlayerProfile profileRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    int id = rs.getInt("player_id");
    return id > 0 ? new PlayerProfile(id, rs.getBoolean("board")) : null;
  }

  private Entry<String, Integer> abilityRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    return Map.entry(rs.getString("ability_name"), rs.getInt("ability_id"));
  }

  private Entry<Integer, String> presetRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    return Map.entry(rs.getInt("preset_id"), rs.getString("preset_name"));
  }

  private boolean nativeUuid() {
    return switch (type()) {
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
