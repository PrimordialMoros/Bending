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
import me.moros.bending.model.CircularQueue;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.user.User;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class SequenceManager {
	private final Map<AbilityDescription, Sequence> registeredSequences = new HashMap<>();
	private final ExpiringMap<User, CircularQueue<AbilityAction>> cache = ExpiringMap.builder()
		.expirationPolicy(ExpirationPolicy.ACCESSED)
		.expiration(10, TimeUnit.SECONDS).build();

	public void clear() {
		cache.clear();
	}

	public boolean registerSequence(AbilityDescription desc, Sequence sequence) {
		// TODO lazy load and verify all required abilities are enabled
		registeredSequences.put(desc, sequence);
		return false;
	}

	public Optional<Sequence> getSequence(AbilityDescription desc) {
		if (desc == null) return Optional.empty();
		return Optional.ofNullable(registeredSequences.get(desc));
	}

	public void registerAction(User user, ActivationMethod action) {
		AbilityDescription desc = user.getSelectedAbility().orElse(null);
		if (desc == null) return;
		CircularQueue<AbilityAction> buffer = cache.computeIfAbsent(user, u -> new CircularQueue<>());
		buffer.add(new AbilityAction(desc, action));
		for (Map.Entry<AbilityDescription, Sequence> entry : registeredSequences.entrySet()) {
			AbilityDescription sequenceDesc = entry.getKey();
			Sequence sequence = entry.getValue();
			if (sequence.matches(buffer)) {
				if (!user.canBend(sequenceDesc)) continue;
				Ability ability = sequenceDesc.createAbility();
				if (ability.activate(user, ActivationMethod.SEQUENCE)) {
					Game.getAbilityManager(user.getWorld()).addAbility(user, ability);
				}
				buffer.clear(); // Consume all actions in the buffer
			}
		}
	}

	/**
	 * Note: this will include hidden abilities. You will need to filter them.
	 * @return a stream of all the registered sequences
	 */
	public Stream<AbilityDescription> getRegisteredSequences() {
		return registeredSequences.keySet().stream();
	}
}
