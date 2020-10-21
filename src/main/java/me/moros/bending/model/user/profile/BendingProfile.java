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

package me.moros.bending.model.user.profile;

import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.UUID;

/**
 * Contains info from the database regarding a bending profile
 * Usually created during AsyncPlayerPreLoginEvent while blocking user's login until done
 * If it takes a long time to load during that event, it will be loaded async after player has logged in instead.
 */
public final class BendingProfile {
	private final UUID uuid;
	private final int id;
	private final BenderData data;
	private boolean board;

	public BendingProfile(@NonNull UUID uuid, @Positive int id, @NonNull BenderData data, boolean board) {
		this.uuid = uuid;
		this.id = id;
		this.data = data;
		this.board = board;
	}

	public BendingProfile(@NonNull UUID uuid, @Positive int id, @NonNull BenderData data) {
		this(uuid, id, data, true);
	}

	public @NonNull UUID getUniqueId() {
		return uuid;
	}

	public @Positive int getInternalId() {
		return id;
	}

	public boolean hasBoard() {
		return board;
	}

	public void setBoard(boolean value) {
		board = value;
	}

	public @NonNull BenderData getData() {
		return data;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof BendingProfile && id == ((BendingProfile) obj).id;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public String toString() {
		return "ID: " + id + "\n" +
			"UUID: " + uuid + "\n" +
			"Board: " + board + "\n" +
			"Elements: " + data.elements.toString() + "\n" +
			"Slots: " + Arrays.toString(data.slots) + "\n" +
			"Presets: " + data.presets.toString();
	}
}
