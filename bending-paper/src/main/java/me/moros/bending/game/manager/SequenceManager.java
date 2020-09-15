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

package me.moros.bending.game.manager;

import me.moros.bending.game.Game;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.AbilityAction;
import me.moros.bending.model.ability.sequence.Action;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Tasker;

import java.util.HashMap;
import java.util.Map;

public final class SequenceManager {
	private final Map<User, Sequence> userSequences = new HashMap<>();
	private final Map<AbilityDescription, Sequence> abilitySequences = new HashMap<>();

	public SequenceManager() {
		Tasker.createTaskTimer(this::update, 20, 20);
	}

	private void update() {
		userSequences.keySet().removeIf(user -> !user.isValid());
	}

	public void clear() {
		userSequences.clear();
	}

	public void registerSequence(AbilityDescription desc, Sequence sequence) {
		abilitySequences.put(desc, sequence);
	}

	public Sequence getSequence(AbilityDescription desc) {
		return abilitySequences.get(desc);
	}

	public void registerAction(User user, Action action) {
		AbilityDescription desc = user.getSelectedAbility().orElse(null);
		if (desc == null) return;
		Sequence userSequence = userSequences.computeIfAbsent(user, u -> new Sequence(new AbilityAction(desc, action)));
		for (Map.Entry<AbilityDescription, Sequence> entry : abilitySequences.entrySet()) {
			AbilityDescription sequenceDesc = entry.getKey();
			Sequence sequence = entry.getValue();
			if (userSequence.matches(sequence)) {
				if (!user.canBend(sequenceDesc)) continue;
				Ability ability = sequenceDesc.createAbility();
				if (ability.activate(user, ActivationMethod.SEQUENCE)) {
					Game.getAbilityInstanceManager(user.getWorld()).addAbility(user, ability);
				}
				userSequences.put(user, new Sequence());
			}
		}
	}
}
