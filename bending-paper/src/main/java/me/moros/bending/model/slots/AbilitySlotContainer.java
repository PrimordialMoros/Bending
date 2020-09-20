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

package me.moros.bending.model.slots;

import me.moros.bending.game.Game;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;

public class AbilitySlotContainer {
	protected AbilityDescription[] abilities;

	public AbilitySlotContainer(int size) {
		this.abilities = new AbilityDescription[size];
	}

	public AbilityDescription getAbility(int slot) {
		if (slot < 1 || slot > abilities.length) return null;
		return abilities[slot - 1];
	}

	/**
	 * Sets the given slot to a specific ability
	 * This has no effect on MultiAbilitySlotContainer
	 * @param slot the slot to put the ability in
	 * @param desc the ability description to put
	 */
	public void setAbility(int slot, AbilityDescription desc) {
		abilities[slot - 1] = desc;
	}

	/**
	 * Makes a preset out of this container
	 * @param name the name of the preset to be created
	 * @return the constructed preset
	 */
	public Preset toPreset(String name) {
		String[] copy = new String[9];
		for (int slot = 0; slot < 9; slot++) {
			AbilityDescription desc = abilities[slot];
			copy[slot] = desc == null ? null : desc.getName();
		}
		return new Preset(0, name, copy);
	}

	public void fromPreset(Preset preset) {
		String[] presetAbilities = preset.getAbilities();
		for (int slot = 0; slot < 9; slot++) {
			abilities[slot] = Game.getAbilityRegistry().getAbilityDescription(presetAbilities[slot]).orElse(null);
		}
	}
}
