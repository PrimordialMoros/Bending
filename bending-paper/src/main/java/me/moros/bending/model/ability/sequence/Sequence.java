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

/**
 * Immutable and thread-safe representation of a sequence
 */
public final class Sequence {
	private final List<AbilityAction> sequence = new ArrayList<>();
	private String instructions = "";

	public Sequence(AbilityAction action, AbilityAction... actions) {
		this.sequence.add(action);
		this.sequence.addAll(Arrays.asList(actions));
	}

	/**
	 * @return Unmodifiable view of this sequence's actions
	 */
	public List<AbilityAction> getActions() {
		return Collections.unmodifiableList(sequence);
	}

	public String getInstructions() {
		if (instructions.isEmpty()) {
			instructions = generateInstructions(this.sequence);
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

	public boolean matches(AbilityAction[] actions) {
		int actionsLength = actions.length - 1;
		int sequenceLength = sequence.size() - 1;
		if (actionsLength < sequenceLength) return false;
		for (int i = 0; i <= sequenceLength; i++) {
			AbilityAction first = sequence.get(sequenceLength - i);
			AbilityAction second = actions[actionsLength - i];
			if (!first.equals(second)) return false;
		}
		return true;
	}
}
