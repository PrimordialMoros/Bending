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

package me.moros.bending.storage;

import com.zaxxer.hikari.HikariDataSource;
import me.moros.bending.Bending;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.PresetHolder;
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.model.user.profile.BendingProfile;
import me.moros.bending.storage.sql.SqlQueries;
import me.moros.bending.storage.sql.SqlStreamReader;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.logging.PluginLogger;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.statement.Batch;
import org.jdbi.v3.core.statement.PreparedBatch;
import org.jdbi.v3.core.statement.Query;

import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class StorageImpl implements BendingStorage {
	private final HikariDataSource dataSource;
	private final StorageType type;
	private PluginLogger logger;
	private final Jdbi DB;

	public StorageImpl(StorageType type, HikariDataSource source) {
		this.type = type;
		dataSource = source;
		DB = Jdbi.create(dataSource);
	}

	@Override
	public @NonNull StorageType getType() {
		return type;
	}

	@Override
	public boolean init(@NonNull PluginLogger logger) {
		this.logger = logger;
		Collection<String> statements;
		try (InputStream stream = Bending.getPlugin().getResource(type.getSchemaPath())) {
			if (stream == null) return false;
			statements = SqlStreamReader.parseQueries(stream);
		} catch (IOException e) {
			this.logger.severe(e.getMessage());
			return false;
		}
		if (!tableExists("bending_players")) {
			DB.useHandle(handle -> {
				Batch batch = handle.createBatch();
				statements.forEach(batch::add);
				batch.execute();
			});
		}
		return true;
	}

	@Override
	public void close() {
		dataSource.close();
	}

	/**
	 * Creates a new profile for the given uuid or returns an existing one if possible.
	 */
	@Override
	public @NonNull BendingProfile createProfile(@NonNull UUID uuid) {
		return loadProfile(uuid).orElseGet(() ->
			DB.withHandle(handle -> {
				int id = (int) handle.createUpdate(SqlQueries.PLAYER_INSERT.getQuery()).bind(0, uuid)
					.executeAndReturnGeneratedKeys().mapToMap().one().get("player_id");
				return new BendingProfile(uuid, id, new BenderData());
			})
		);
	}

	/**
	 * This method will attempt to load a profile from the database and execute the consumer if found.
	 * @param uuid the player's uuid
	 * @param consumer the consumer to executre if a profile was found
	 * @see #createProfile(UUID)
	 */
	public void loadProfileAsync(@NonNull UUID uuid, @NonNull Consumer<BendingProfile> consumer) {
		Tasker.newChain().asyncFirst(() -> loadProfile(uuid)).asyncLast(p -> p.ifPresent(consumer)).execute();
	}

	/**
	 * Asynchronously saves the given bendingPlayer's data to the database.
	 * It updates the profile and stores the current elements and bound abilities.
	 * @param bendingPlayer the BendingPlayer to save
	 */
	public void savePlayerAsync(@NonNull BendingPlayer bendingPlayer) {
		Tasker.newChain().async(() -> {
			updateProfile(bendingPlayer.getProfile());
			saveElements(bendingPlayer);
			saveSlots(bendingPlayer);
		}).execute();
	}

	/**
	 * Adds all given elements to the database
	 * @param elements the elements to add
	 */
	@Override
	public boolean createElements(@NonNull Set<@NonNull Element> elements) {
		try {
			DB.useHandle(handle -> {
				PreparedBatch batch = handle.prepareBatch(SqlQueries.ELEMENTS_INSERT_NEW.getQuery());
				for (Element element : elements) {
					batch.bind(0, element.name()).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return false;
	}

	/**
	 * Adds all given abilities to the database
	 * @param abilities the abilities to add
	 */
	@Override
	public boolean createAbilities(@NonNull Set<@NonNull AbilityDescription> abilities) {
		try {
			DB.useHandle(handle -> {
				PreparedBatch batch = handle.prepareBatch(SqlQueries.ABILITIES_INSERT_NEW.getQuery());
				for (AbilityDescription desc : abilities) {
					batch.bind(0, desc.getName()).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return false;
	}

	/**
	 * This is currently loaded asynchronously using a LoadingCache
	 * @see PresetHolder
	 */
	@Override
	public @Nullable Preset loadPreset(int playerId, @NonNull String name) {
		int presetId = getPresetId(playerId, name);
		if (presetId == 0) return null;
		String[] abilities = new String[9];
		try {
			return DB.withHandle(handle -> {
				Query query = handle.createQuery(SqlQueries.PRESET_SLOTS_SELECT_BY_ID.getQuery()).bind(0, presetId);
				for (Map<String, Object> map : query.mapToMap()) {
					int slot = (int) map.get("slot");
					String abilityName = (String) map.get("ability_name");
					abilities[slot - 1] = abilityName;
				}
				return new Preset(presetId, name, abilities);
			});
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return null;
	}

	public void savePresetAsync(int playerId, @NonNull Preset preset, @NonNull Consumer<Boolean> consumer) {
		Tasker.newChain().asyncFirst(() -> savePreset(playerId, preset))
			.abortIfNull().asyncLast(consumer::accept).execute();
	}

	public void deletePresetAsync(int presetId) {
		Tasker.newChain().asyncFirst(() -> deletePreset(presetId)).execute();
	}

	private Optional<BendingProfile> loadProfile(UUID uuid) {
		try {
			return DB.withHandle(handle -> {
				Optional<Map<String, Object>> result = handle.createQuery(SqlQueries.PLAYER_SELECT_BY_UUID.getQuery()).bind(0, uuid).mapToMap().findOne();
				if (!result.isPresent()) return Optional.empty();
				int id = (int) result.get().get("player_id");
				boolean board = (boolean) result.get().getOrDefault("board", true);
				BenderData data = new BenderData(getSlots(id), getElements(id), getPresets(id));
				return Optional.of(new BendingProfile(uuid, id, data, board));
			});
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return Optional.empty();
	}

	private boolean updateProfile(BendingProfile profile) {
		try {
			DB.useHandle(handle ->
				handle.createUpdate(SqlQueries.PLAYER_UPDATE_BOARD_FOR_ID.getQuery())
					.bind(0, profile.hasBoard()).bind(1, profile.getInternalId()).execute()
			);
			return true;
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return false;
	}

	private boolean saveElements(BendingPlayer player) {
		int id = player.getProfile().getInternalId();
		try {
			DB.useHandle(handle -> {
				handle.createUpdate(SqlQueries.PLAYER_ELEMENTS_REMOVE_FOR_ID.getQuery()).bind(0, id).execute();
				PreparedBatch batch = handle.prepareBatch(SqlQueries.PLAYER_ELEMENTS_INSERT_FOR_NAME.getQuery());
				for (Element element : player.getElements()) {
					batch.bind(0, id).bind(1, element.name()).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
		return false;
	}

	private boolean saveSlots(BendingPlayer player) {
		int id = player.getProfile().getInternalId();
		Preset temp = player.createPresetFromSlots("");
		try {
			DB.useHandle(handle -> {
				handle.createUpdate(SqlQueries.PLAYER_SLOTS_REMOVE_FOR_ID.getQuery()).bind(0, id).execute();
				PreparedBatch batch = handle.prepareBatch(SqlQueries.PLAYER_SLOTS_INSERT_NEW.getQuery());
				for (int slot = 0; slot < temp.getAbilities().length; slot++) {
					int abilityId = getAbilityId(temp.getAbilities()[slot]);
					if (abilityId == 0) continue;
					batch.bind(0, id).bind(1, slot + 1).bind(2, abilityId).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return false;
	}

	private boolean savePreset(int playerId, Preset preset) {
		if (preset.getInternalId() > 0) return false; // Must be a new preset!
		if (!deletePreset(playerId, preset.getName())) return false; // needed for overwriting
		try {
			DB.useHandle(handle -> {
				int presetId = (int) handle.createUpdate(SqlQueries.PRESET_INSERT_NEW.getQuery())
					.bind(0, playerId).bind(1, preset.getName())
					.executeAndReturnGeneratedKeys()
					.mapToMap().one().get("preset_id");
				String[] abilities = preset.getAbilities();
				PreparedBatch batch = handle.prepareBatch(SqlQueries.PRESET_SLOTS_INSERT_NEW.getQuery());
				batch.execute();
				for (int i = 0; i < 9; i++) {
					String abilityName = abilities[i];
					if (abilityName == null) continue;
					int abilityId = getAbilityId(abilityName);
					if (abilityId == 0) continue;
					batch.bind(0, presetId).bind(1, i + 1).bind(2, abilityId).add();
				}
				batch.execute();
			});
			return true;
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return false;
	}

	private boolean deletePreset(int presetId) {
		if (presetId <= 0) return false; // It won't exist
		try {
			DB.useHandle(handle ->
				handle.createUpdate(SqlQueries.PRESET_REMOVE_FOR_ID.getQuery()).bind(0, presetId).execute()
			);
			return true;
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return false;
	}

	private int getAbilityId(String name) {
		if (name == null) return 0;
		try {
			return DB.withHandle(handle ->
				(int) handle.createQuery(SqlQueries.ABILITIES_SELECT_ID_BY_NAME.getQuery()).bind(0, name).mapToMap().one().get("ability_id")
			);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return 0;
	}

	private String[] getSlots(int playerId) {
		String[] slots = new String[9];
		try {
			DB.useHandle(handle -> {
				Query query = handle.createQuery(SqlQueries.PLAYER_SLOTS_SELECT_FOR_ID.getQuery()).bind(0, playerId);
				for (Map<String, Object> map : query.mapToMap()) {
					int slot = (int) map.get("slot");
					String abilityName = (String) map.get("ability_name");
					slots[slot - 1] = abilityName;
				}
			});
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return slots;
	}

	private Set<String> getElements(int playerId) {
		try {
			return DB.withHandle(handle ->
				handle.createQuery(SqlQueries.PLAYER_ELEMENTS_SELECT_FOR_ID.getQuery()).bind(0, playerId)
					.mapToMap().stream().map(r -> (String) r.get("element_name")).collect(Collectors.toSet())
			);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return Collections.emptySet();
	}

	private Set<String> getPresets(int playerId) {
		try {
			return DB.withHandle(handle ->
				handle.createQuery(SqlQueries.PRESET_NAMES_SELECT_BY_PLAYER_ID.getQuery()).bind(0, playerId)
					.mapToMap().stream().map(r -> (String) r.get("preset_name")).collect(Collectors.toSet())
			);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return Collections.emptySet();
	}

	private boolean deletePreset(int playerId, String presetName) {
		try {
			DB.withHandle(handle ->
				handle.createUpdate(SqlQueries.PRESET_REMOVE_SPECIFIC.getQuery()).bind(0, playerId).bind(1, presetName)
			);
			return true;
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return false;
	}

	// Gets preset id and returns 0 if doesn't exist or when a problem occurs.
	private int getPresetId(int playerId, String presetName) {
		try {
			return DB.withHandle(handle -> {
				Query query = handle.createQuery(SqlQueries.PRESET_SELECT_ID_BY_ID_AND_NAME.getQuery())
					.bind(0, playerId).bind(1, presetName);
				return (int) query.mapToMap().one().get("preset_id");
			});
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
		return 0;
	}

	private boolean tableExists(String table) {
		try (ResultSet rs = dataSource.getConnection().getMetaData().getTables(null, null, "%", null)) {
			while (rs.next()) {
				if (rs.getString(3).equalsIgnoreCase(table)) {
					return true;
				}
			}
		} catch (SQLException e) {
			logger.warn(e.getMessage());
		}
		return false;
	}
}
