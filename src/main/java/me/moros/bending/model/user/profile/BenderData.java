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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.Set;

/**
 * Holds data from the database that are needed to construct the BendingPlayer object
 */
public final class BenderData {
	public final String[] slots;
	public final Set<String> elements;
	public final Set<String> presets;

	public BenderData(@NonNull String[] slots, @NonNull Set<@NonNull String> elements, @NonNull Set<@NonNull String> presets) {
		this.slots = slots;
		this.elements = elements;
		this.presets = presets;
	}

	public BenderData() {
		this(new String[9], Collections.emptySet(), Collections.emptySet());
	}
}
