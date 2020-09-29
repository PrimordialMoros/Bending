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

package me.moros.bending.game;

import me.moros.bending.Bending;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Holds all the registered AbilityDescriptions for the current session.
 */
public final class AbilityRegistry {
	private final Map<String, AbilityDescription> abilities = new HashMap<>();
	private final EnumMap<Element, Set<AbilityDescription>> passives = new EnumMap<>(Element.class);

	protected int registerAbilities(Collection<AbilityDescription> abilities) {
		int counter = 0;
		for (AbilityDescription desc : abilities) {
			if (registerAbility(desc)) counter++;
		}
		return counter;
	}

	private boolean registerAbility(AbilityDescription desc) {
		if (desc == null) return false;
		if (!desc.getConfigNode().getNode("enabled").getBoolean(true)) {
			Bending.getLog().info(desc.getName() + " is disabled.");
			return false;
		}
		abilities.put(desc.getName().toLowerCase(), desc);
		if (desc.isActivatedBy(ActivationMethod.PASSIVE)) {
			passives.computeIfAbsent(desc.getElement(), e -> new HashSet<>()).add(desc);
		}
		return true;
	}

	/**
	 * Check if an ability is enabled and registered.
	 * @param desc the ability to check
	 * @return result
	 */
	public boolean isValid(AbilityDescription desc) {
		return desc != null && abilities.containsKey(desc.getName().toLowerCase());
	}

	/**
	 * Note: this will include hidden abilities. You will need to filter them.
	 * @return a stream of all the abilities in this registry
	 */
	public Stream<AbilityDescription> getAbilities() {
		return abilities.values().stream();
	}

	/**
	 * Note: this will include hidden passives. You will need to filter them.
	 * @return a stream of all the passives in this registry
	 */
	public Stream<AbilityDescription> getPassives(Element element) {
		return passives.getOrDefault(element, Collections.emptySet()).stream();
	}

	/**
	 * @param name the name to match
	 * @return Optional ability description
	 * @see #getAbilityDescription(Ability)
	 */
	public Optional<AbilityDescription> getAbilityDescription(String name) {
		if (name == null || name.isEmpty()) return Optional.empty();
		return Optional.ofNullable(abilities.get(name.toLowerCase()));
	}

	/**
	 * This should only be used by {@link Ability#getDescription}
	 * @param ability the ability to match
	 * @return the ability description that matches the specified ability by name or null if not found
	 * @see #getAbilityDescription(String)
	 */
	public AbilityDescription getAbilityDescription(Ability ability) {
		return abilities.get(ability.getName().toLowerCase());
	}
}
