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

package me.moros.bending.storage.implementation.sql;

import co.aikar.idb.DB;
import co.aikar.idb.DbRow;
import me.moros.bending.Bending;
import me.moros.bending.model.user.player.BenderData;
import me.moros.bending.model.user.player.BendingProfile;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.storage.StorageType;
import me.moros.bending.storage.implementation.StorageImplementation;
import me.moros.bending.storage.sql.SqlQueries;
import me.moros.bending.storage.sql.SqlStreamReader;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SqlStorage implements StorageImplementation {
	private final StorageType type;

	public SqlStorage(StorageType type) {
		this.type = type;
	}

	@Override
	public StorageType getType() {
		return type;
	}

	@Override
	public void init() {
		Collection<String> statements;
		try (InputStream stream = Bending.getPlugin().getResource(type.getSchemaPath())) {
			statements = SqlStreamReader.parseQueries(stream);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		try (Statement stm = DB.getGlobalDatabase().getConnection().createStatement()) {
			for (String query : statements) {
				stm.addBatch(query);
			}
			stm.executeBatch();
		} catch (SQLException exception) {
			exception.printStackTrace();
		}
	}

	@Override
	public BendingProfile createProfile(UUID uuid) {
		return loadProfile(uuid).orElseGet(() -> {
			try {
				DbRow row = DB.getFirstRow(SqlQueries.PLAYER_INSERT.getQuery(), uuid); // Returns the id;
				int id = row.getInt("player_id");
				return new BendingProfile(uuid, id, new BenderData());
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		});
	}

	@Override
	public Optional<BendingProfile> loadProfile(UUID uuid) {
		try {
			DbRow row = DB.getFirstRow(SqlQueries.PLAYER_SELECT_BY_UUID.getQuery(), uuid);
			if (row == null || row.isEmpty()) return Optional.empty();
			int id = row.getInt("player_id", 0);
			boolean board = row.get("board", true);
			BenderData data = new BenderData(getSlots(id), getElements(id), getPresets(id));
			return Optional.of(new BendingProfile(uuid, id, data, board));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Optional.empty();
	}

	@Override
	public boolean updateProfile(BendingProfile profile) {
		try {
			DB.executeUpdate(SqlQueries.PLAYER_UPDATE_BOARD_FOR_ID.getQuery(), profile.hasBoard(), profile.getInternalId());
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean createElements(Set<Element> elements) {
		try {
			for (Element element : elements) {
				DB.executeInsert(SqlQueries.ELEMENTS_INSERT_NEW.getQuery(), element.name());
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean createAbilities(Set<AbilityDescription> abilities) {
		try {
			for (AbilityDescription desc : abilities) {
				DB.executeInsert(SqlQueries.ABILITIES_INSERT_NEW.getQuery(), desc.getName());
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean saveElements(BendingPlayer player) {
		try {
			int id = player.getProfile().getInternalId();
			DB.executeUpdate(SqlQueries.PLAYER_ELEMENTS_REMOVE_FOR_ID.getQuery(), id);
			for (Element element : player.getElements()) {
				DB.executeInsert(SqlQueries.PLAYER_ELEMENTS_INSERT_FOR_NAME.getQuery(), id, element.name());
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean saveSlot(BendingPlayer player, int slotIndex) {
		try {
			int id = player.getProfile().getInternalId();
			DB.executeUpdate(SqlQueries.PLAYER_SLOTS_REMOVE_SPECIFIC.getQuery(), id, slotIndex);
			Optional<AbilityDescription> desc = player.getStandardSlotAbility(slotIndex);
			if (!desc.isPresent()) return false;
			int abilityId = getAbilityId(desc.get().getName());
			if (abilityId == 0) return false;
			DB.executeUpdate(SqlQueries.PLAYER_SLOTS_INSERT_NEW.getQuery(), id, slotIndex, abilityId);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Returns null if doesn't exist or when a problem occurs.
	@Override
	public Preset loadPreset(int playerId, String name) {
		int presetId = getPresetId(playerId, name);
		if (presetId == 0) return null;
		try {
			String[] abilities = new String[9];
			for (DbRow row : DB.getResults(SqlQueries.PRESET_SLOTS_SELECT_BY_ID.getQuery(), presetId)) {
				int slot = row.getInt("slot");
				String abilityName = row.getString("ability_name");
				abilities[slot - 1] = abilityName;
			}
			return new Preset(presetId, name, abilities);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean savePreset(int playerId, Preset preset) {
		if (preset.getInternalId() > 0) return false; // Must be a new preset!
		if (!deletePreset(playerId, preset.getName())) return false; // needed for overwriting
		try {
			// Create empty preset and get the actual preset id
			int presetId = DB.getFirstRow(SqlQueries.PRESET_INSERT_NEW.getQuery(), playerId, preset.getName()).getInt("preset_id");
			int index = 0;
			for (String ability : preset.getAbilities()) {
				if (ability != null) {
					int abilityId = getAbilityId(ability);
					if (abilityId == 0) return false;
					DB.executeUpdate(SqlQueries.PRESET_SLOTS_INSERT_NEW.getQuery(), presetId, index, abilityId);
				}
				index++;
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean deletePreset(int presetId) {
		if (presetId <= 0) return false; // It won't exist
		try {
			DB.executeUpdate(SqlQueries.PRESET_REMOVE_FOR_ID.getQuery(), presetId);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Helper methods
	private int getAbilityId(String name) {
		try {
			DbRow row = DB.getFirstRow(SqlQueries.ABILITIES_SELECT_ID_BY_NAME.getQuery(), name);
			if (row == null || row.isEmpty()) return 0;
			return row.getInt("ability_id", 0);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private String[] getSlots(int playerId) {
		String[] slots = new String[9];
		try {
			for (DbRow row2 : DB.getResults(SqlQueries.PLAYER_SLOTS_SELECT_FOR_ID.getQuery(), playerId)) {
				int slot = row2.getInt("slot");
				String abilityName = row2.getString("ability_name");
				slots[slot - 1] = abilityName;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return slots;
	}

	private Set<String> getElements(int playerId) {
		try {
			return DB.getResults(SqlQueries.PLAYER_ELEMENTS_SELECT_FOR_ID.getQuery(), playerId).stream()
				.map(r -> r.getString("element_name")).collect(Collectors.toSet());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}

	private Set<String> getPresets(int playerId) {
		try {
			return DB.getResults(SqlQueries.PRESET_NAMES_SELECT_BY_PLAYER_ID.getQuery(), playerId).stream()
				.map(row -> row.getString("preset_name"))
				.collect(Collectors.toSet());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return Collections.emptySet();
	}

	private boolean deletePreset(int id, String name) {
		try {
			DB.executeUpdate(SqlQueries.PRESET_REMOVE_SPECIFIC.getQuery(), id, name);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	// Gets preset id
	// Returns 0 if doesn't exist or when a problem occurs.
	private int getPresetId(int id, String name) {
		try {
			return DB.getFirstRow(SqlQueries.PRESET_SELECT_ID_BY_ID_AND_NAME.getQuery(), id, name).getInt("preset_id", 0);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
}
