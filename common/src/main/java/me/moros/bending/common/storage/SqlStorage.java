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

package me.moros.bending.common.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.util.collect.ElementSet;
import me.moros.bending.common.logging.Logger;
import me.moros.bending.common.storage.sql.PresetAccumulator;
import me.moros.bending.common.storage.sql.dialect.SqlDialect;
import me.moros.bending.common.storage.sql.migration.V1__Rename_legacy_tables;
import me.moros.bending.common.storage.sql.migration.V3__Migrate_from_legacy;
import me.moros.bending.common.util.UUIDUtil;
import me.moros.storage.StorageDataSource;
import net.kyori.adventure.util.Index;
import org.flywaydb.core.Flyway;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.AbstractArgumentFactory;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.StatementContext;
import org.jspecify.annotations.Nullable;

final class SqlStorage extends AbstractStorage {
  private final Index<UUID, AbilityDescription> abilityIndex;

  private final StorageDataSource dataSource;
  private final SqlDialect dialect;
  private final Jdbi DB;

  SqlStorage(Logger logger, StorageDataSource dataSource) {
    super(logger);
    this.dataSource = dataSource;
    this.dialect = SqlDialect.createFor(logger, dataSource);
    migrateWithFlyway();
    this.DB = Jdbi.create(this.dataSource.source());
    if (!dialect.nativeUuid()) {
      DB.registerArgument(new UUIDArgumentFactory());
      DB.registerColumnMapper(UUID.class, new BinaryUUIDColumnMapper());
    }
    this.abilityIndex = createAbilities();
  }

  private void migrateWithFlyway() {
    Flyway flyway = Flyway.configure(getClass().getClassLoader())
      .table("bending_schemahistory").loggers("slf4j").locations("classpath:bending/migrations")
      .javaMigrations(new V1__Rename_legacy_tables(), new V3__Migrate_from_legacy(logger, dialect.nativeUuid()))
      .dataSource(dataSource.source()).validateOnMigrate(true).validateMigrationNaming(true)
      .baselineOnMigrate(true).baselineVersion("0")
      .placeholders(Map.of(
        "extraTableOptions", dialect.extraTableOptions(),
        "uuidType", dialect.uuidType(),
        "defineElementEnumType", dialect.defineElementEnumType(),
        "elementEnumType", dialect.elementEnumType()
      )).load();
    flyway.migrate();
  }

  private Index<UUID, AbilityDescription> createAbilities() {
    var entries = DB.inTransaction(handle -> {
      Map<AbilityDescription, UUID> map = handle.createQuery(dialect.SELECT_ABILITIES)
        .map(this::abilityRowMapper).filter(Objects::nonNull).collectToMap(Entry::getKey, Entry::getValue);
      int size = map.size();
      PreparedBatch batch = handle.prepareBatch(dialect.insertAbilities());
      for (AbilityDescription desc : Registries.ABILITIES) {
        if (desc.canBind() && !map.containsKey(desc)) {
          UUID uuid = map.computeIfAbsent(desc, k -> UUID.randomUUID());
          batch.bind(0, uuid).bind(1, desc.key().asString()).add();
        }
      }
      if (map.size() != size) {
        batch.execute();
      }
      return map;
    });
    return Index.create(entries::get, new ArrayList<>(entries.keySet()));
  }

  @Override
  public Set<UUID> loadUuids() {
    return DB.withHandle(handle -> handle.createQuery(dialect.SELECT_ALL_USER_UUIDS).mapTo(UUID.class).set());
  }

  @Override
  public @Nullable BenderProfile loadProfile(UUID uuid) {
    Boolean board = DB.withHandle(handle -> handle.createQuery(dialect.SELECT_USER_BY_UUID)
      .bind(0, uuid).mapTo(boolean.class).findOne().orElse(null));
    if (board == null) {
      return null;
    }
    Set<Element> elements = getElements(uuid);
    Map<String, Preset> presetMap = getSlotsAndPresets(uuid);
    Preset slots = presetMap.remove("");
    if (slots == null) {
      slots = Preset.empty();
    }
    return BenderProfile.of(uuid, board, elements, slots, presetMap.values());
  }

  @Override
  public boolean saveProfile(BenderProfile profile) {
    saveBoard(profile);
    saveElements(profile);
    savePresets(profile);
    return true;
  }

  @Override
  public boolean isRemote() {
    return !dataSource.type().isLocal();
  }

  @Override
  public void close() {
    dataSource.source().close();
  }

  @Override
  public String toString() {
    return dataSource.type().toString();
  }

  private Set<Element> getElements(UUID uuid) {
    return DB.withHandle(handle ->
      handle.createQuery(dialect.SELECT_USER_ELEMENTS).bind(0, uuid)
        .mapTo(String.class).map(Element::fromName).filter(Objects::nonNull).toCollection(ElementSet::mutable)
    );
  }

  private Map<String, Preset> getSlotsAndPresets(UUID uuid) {
    return DB.withHandle(handle ->
      handle.createQuery(dialect.SELECT_USER_PRESETS).bind(0, uuid)
        .reduceRows(new PresetAccumulator(this::getAbilityFromId))
        .collect(Collectors.toMap(Preset::name, Function.identity()))
    );
  }

  private void saveBoard(BenderProfile profile) {
    DB.useTransaction(handle ->
      handle.createUpdate(dialect.insertUser()).bind(0, profile.uuid())
        .bind(1, profile.board()).execute()
    );
  }

  private void saveElements(BenderProfile profile) {
    DB.useTransaction(handle -> {
      handle.createUpdate(dialect.REMOVE_USER_ELEMENTS).bind(0, profile.uuid()).execute();
      if (!profile.elements().isEmpty()) {
        PreparedBatch batch = handle.prepareBatch(dialect.INSERT_USER_ELEMENTS);
        for (Element element : profile.elements()) {
          batch.bind(0, profile.uuid()).bind(1, element.name().toLowerCase(Locale.ROOT)).add();
        }
        batch.execute();
      }
    });
  }

  private void savePresets(BenderProfile profile) {
    // Loading the currently stored presets to only save the difference
    UUID userId = profile.uuid();
    var oldPresets = getSlotsAndPresets(userId).values();
    var newPresets = profile.presets().values();

    Set<Preset> removed = new HashSet<>(oldPresets);
    removed.removeAll(newPresets);
    removed.remove(profile.slots());

    Set<Preset> added = new HashSet<>(newPresets);
    added.add(profile.slots());
    added.removeAll(oldPresets);
    added.removeIf(Preset::isEmpty);

    deletePresets(userId, removed);
    savePresets(userId, added);
  }

  private void savePresets(UUID userId, Collection<Preset> presets) {
    if (presets.isEmpty()) {
      return;
    }
    DB.useTransaction(handle -> {
      PreparedBatch presetBatch = handle.prepareBatch(dialect.INSERT_USER_PRESET_WITH_ID);
      PreparedBatch presetSlotBatch = handle.prepareBatch(dialect.INSERT_USER_PRESET_SLOTS);
      for (Preset preset : presets) {
        UUID presetId = UUID.randomUUID();
        presetBatch.bind(0, presetId).bind(1, userId).bind(2, preset.name()).add();
        preset.forEach((desc, idx) -> presetSlotBatch
          .bind(0, presetId)
          .bind(1, idx + 1)
          .bind(2, abilityIndex.key(desc))
          .add()
        );
      }
      presetBatch.execute();
      presetSlotBatch.execute();
    });
  }

  private void deletePresets(UUID userId, Collection<Preset> presets) {
    if (presets.isEmpty()) {
      return;
    }
    DB.useTransaction(handle -> {
      PreparedBatch batch = handle.prepareBatch(dialect.REMOVE_USER_PRESET);
      for (Preset preset : presets) {
        batch.bind(0, userId).bind(1, preset.name()).add();
      }
      batch.execute();
    });
  }

  private @Nullable AbilityDescription getAbilityFromId(UUID uuid) {
    return abilityIndex.value(uuid);
  }

  private @Nullable Entry<AbilityDescription, UUID> abilityRowMapper(ResultSet rs, StatementContext ctx) throws SQLException {
    AbilityDescription desc = Registries.ABILITIES.fromString(rs.getString("ability_name"));
    return desc == null ? null : Map.entry(desc, mapUuid(rs, "ability_id", ctx));
  }

  private UUID mapUuid(ResultSet rs, String column, StatementContext ctx) throws SQLException {
    return dialect.nativeUuid() ? rs.getObject(column, UUID.class) :
      ctx.findColumnMapperFor(UUID.class).orElseThrow().map(rs, column, ctx);
  }

  private static final class UUIDArgumentFactory extends AbstractArgumentFactory<UUID> {
    private UUIDArgumentFactory() {
      super(Types.BINARY);
    }

    @Override
    protected Argument build(UUID value, ConfigRegistry config) {
      return (position, statement, ctx) -> statement.setBytes(position, UUIDUtil.toBytes(value));
    }
  }

  private static final class BinaryUUIDColumnMapper implements ColumnMapper<UUID> {
    @Override
    public @Nullable UUID map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
      byte[] bytes = rs.getBytes(columnNumber);
      return bytes == null ? null : UUIDUtil.fromBytes(bytes);
    }
  }
}
