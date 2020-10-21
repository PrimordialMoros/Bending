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

package me.moros.bending.model.user;

import me.moros.bending.Bending;
import me.moros.bending.events.ElementChangeEvent;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.predicates.conditionals.CompositeBendingConditional;
import me.moros.bending.model.slots.AbilitySlotContainer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.value.qual.IntRange;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

public interface User extends BukkitUser {
	@NonNull ElementHolder getElementHolder();

	default boolean hasElement(@NonNull Element element) {
		return getElementHolder().hasElement(element);
	}

	default boolean addElement(@NonNull Element element) {
		if (getElementHolder().addElement(element)) {
			Bending.getEventBus().postElementChangeEvent(this, ElementChangeEvent.Result.ADD);
			return true;
		}
		return false;
	}

	default boolean removeElement(@NonNull Element element) {
		if (getElementHolder().removeElement(element)) {
			validateSlots();
			Bending.getEventBus().postElementChangeEvent(this, ElementChangeEvent.Result.REMOVE);
			return true;
		}
		return false;
	}

	default @NonNull Set<@NonNull Element> getElements() {
		return getElementHolder().getElements();
	}

	default boolean setElement(@NonNull Element element) {
		getElementHolder().clear();
		getElementHolder().addElement(element);
		validateSlots();
		Bending.getGame().getAbilityManager(getWorld()).clearPassives(this);
		Bending.getGame().getAbilityManager(getWorld()).createPassives(this);
		Bending.getEventBus().postElementChangeEvent(this, ElementChangeEvent.Result.CHOOSE);
		return true;
	}

	boolean isOnCooldown(@NonNull AbilityDescription desc);

	void setCooldown(@NonNull AbilityDescription desc, long duration);

	/**
	 * Like setSlotAbility but won't call any events
	 */
	void setSlotAbilityInternal(@IntRange(from = 1, to = 9) int slot, @Nullable AbilityDescription desc);

	/**
	 * This is to be used when setting individual slots.
	 * If you want to bind or change multiple slots then use dummy presets
	 */
	void setSlotAbility(@IntRange(from = 1, to = 9) int slot, @Nullable AbilityDescription desc);

	Optional<AbilityDescription> getSlotAbility(@IntRange(from = 1, to = 9) int slot);

	Optional<AbilityDescription> getSelectedAbility();

	default void clearSlot(@IntRange(from = 1, to = 9) int slot) {
		setSlotAbility(slot, null);
	}

	void addSlotContainer(@NonNull AbilitySlotContainer slotContainer);

	void removeLastSlotContainer();

	@NonNull CompositeBendingConditional getBendingConditional();

	default boolean canBend(@NonNull AbilityDescription desc) {
		return getBendingConditional().test(this, desc);
	}

	/**
	 * Checks bound abilities and clears any invalid ability slots.
	 * A slot is considered invalid if the user doesn't have the ability's element or doesn't have its permission.
	 */
	default void validateSlots() {
		IntStream.rangeClosed(1, 9).forEach(i -> getSlotAbility(i).ifPresent(desc -> {
			if (!hasElement(desc.getElement()) || !hasPermission(desc)) setSlotAbilityInternal(i, null);
		}));
	}
}
