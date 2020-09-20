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

import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Sequence {
	private final List<AbilityAction> actions = new ArrayList<>();
	private String instructions = "";

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

	public String getInstructions() {
		if (instructions.isEmpty()) {
			instructions = generateInstructions(this.actions);
		}
		return instructions;
	}

	private static String generateInstructions(List<AbilityAction> actions) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < actions.size(); i++) {
			AbilityAction abilityAction = actions.get(i);
			if (i != 0) sb.append(" > ");
			AbilityDescription desc = abilityAction.getAbilityDescription();
			ActivationMethod action = abilityAction.getAction();
			String actionString = action.toString();
			if (action == ActivationMethod.SNEAK && i + 1 < actions.size()) {
				// Check if the next instruction is to release this sneak.
				AbilityAction next = actions.get(i + 1);
				if (desc.equals(next.getAbilityDescription()) && next.getAction() == ActivationMethod.SNEAK_RELEASE) {
					actionString = "Tap Sneak";
					i++;
				}
			}
			sb.append(desc.getName()).append(" (").append(actionString).append(")");
		}
		return sb.toString();
	}
}
