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

import org.apache.commons.math3.util.FastMath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Sequence {
	private static final int MAX_SEQUENCE_SIZE = 10;
	private List<AbilityAction> actions = new ArrayList<>();

	public Sequence() {
	}

	public Sequence(AbilityAction action, AbilityAction... actions) {
		this.actions.addAll(Arrays.asList(actions));
		this.actions.add(action);
		if (this.actions.size() > MAX_SEQUENCE_SIZE) {
			this.actions = this.actions.subList(1, MAX_SEQUENCE_SIZE + 1);
		}
	}

	public Sequence addAction(AbilityAction action) {
		return new Sequence(action, actions.toArray(new AbilityAction[0]));
	}

	public boolean matches(Sequence other) {
		if (actions.size() < other.actions.size()) return false;
		for (int i = 0; i < FastMath.min(actions.size(), other.actions.size()); ++i) {
			// Check for matches backward so the latest actions are included.
			AbilityAction first = actions.get(actions.size() - 1 - i);
			AbilityAction second = other.actions.get(other.actions.size() - 1 - i);

			if (!first.equals(second)) {
				return false;
			}
		}
		return true;
	}

	public int size() {
		return actions.size();
	}

	public List<AbilityAction> getActions() {
		return actions;
	}
}
