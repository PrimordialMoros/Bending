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

package me.moros.bending.model.ability.sequence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Sequence {
	private final List<AbilityAction> actions = new ArrayList<>();

	public Sequence(AbilityAction action, AbilityAction... actions) {
		this.actions.add(action);
		this.actions.addAll(Arrays.asList(actions));
	}

	public int size() {
		return actions.size();
	}

	/**
	 * @return Unmodifiable and thread-safe view of this sequence's actions
	 */
	public List<AbilityAction> getActions() {
		return Collections.unmodifiableList(actions);
	}
}
