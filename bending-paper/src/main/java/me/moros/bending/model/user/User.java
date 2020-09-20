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
import me.moros.bending.game.Game;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicates.conditionals.CompositeBendingConditional;
import me.moros.bending.model.slots.AbilitySlotContainer;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

public interface User {
	ElementHolder getElementHolder();

	default boolean hasElement(Element element) {
		if (element == null) return false;
		return getElementHolder().hasElement(element);
	}

	default boolean addElement(Element element) {
		if (getElementHolder().addElement(element)) {
			Bending.getEventBus().postElementChangeEvent(this, ElementChangeEvent.Result.ADD);
			return true;
		}
		return false;
	}

	default boolean removeElement(Element element) {
		if (getElementHolder().removeElement(element)) {
			validateSlots();
			Bending.getEventBus().postElementChangeEvent(this, ElementChangeEvent.Result.REMOVE);
			return true;
		}
		return false;
	}

	default Set<Element> getElements() {
		return getElementHolder().getElements();
	}

	default boolean setElement(Element element) {
		if (element == null) return false;
		getElementHolder().clear();
		getElementHolder().addElement(element);
		validateSlots();
		Game.getAbilityManager(getWorld()).clearPassives(this);
		Game.getAbilityManager(getWorld()).createPassives(this);
		Bending.getEventBus().postElementChangeEvent(this, ElementChangeEvent.Result.CHOOSE);
		return true;
	}

	/**
	 * Like setSlotAbility but won't call any events
	 */
	void setSlotAbilityInternal(int slot, AbilityDescription desc);

	/**
	 * This is to be used when setting individual slots.
	 * If you want to bind or change multiple slots then use dummy presets
	 */
	void setSlotAbility(int slot, AbilityDescription desc);

	Optional<AbilityDescription> getSlotAbility(int slot);

	Optional<AbilityDescription> getSelectedAbility();

	default void clearSlot(int slot) {
		setSlotAbility(slot, null);
	}

	default void clearSlots() {
		IntStream.rangeClosed(1, 9).forEach(this::clearSlot);
	}

	void addSlotContainer(AbilitySlotContainer slotContainer);

	void removeLastSlotContainer();

	boolean isOnCooldown(AbilityDescription desc);

	void setCooldown(AbilityDescription desc, long duration);

	default void setCooldown(AbilityDescription desc) {
		if (desc != null) setCooldown(desc, desc.getCooldown());
	}

	default void setCooldown(Ability ability) {
		if (ability != null) setCooldown(ability.getDescription());
	}

	default void setCooldown(Ability ability, long duration) {
		if (ability != null) setCooldown(ability.getDescription(), duration);
	}

	boolean canBend(AbilityDescription desc);

	CompositeBendingConditional getBendingConditional();

	boolean hasPermission(String permission);

	default boolean hasPermission(AbilityDescription desc) {
		if (desc == null) return false;
		return hasPermission(desc.getPermission());
	}

	default boolean isSpectator() {
		return false;
	}

	LivingEntity getEntity();

	default Block getHeadBlock() {
		return getEntity().getEyeLocation().getBlock();
	}

	default Vector3 getLocation() {
		return new Vector3(getEntity().getLocation());
	}

	default Vector3 getEyeLocation() {
		return new Vector3(getEntity().getEyeLocation());
	}

	default Vector3 getDirection() {
		return new Vector3(getEntity().getLocation().getDirection());
	}

	default World getWorld() {
		return getEntity().getWorld();
	}

	/**
	 * @return Entity::isValid or Player::isOnline if user is a player
	 */
	default boolean isValid() {
		return getEntity().isValid();
	}

	default boolean isDead() {
		return getEntity().isDead();
	}

	default boolean isSneaking() {
		return true; // Non-players are always considered sneaking so they can charge abilities.
	}

	default boolean getAllowFlight() {
		return true;
	}

	default boolean isFlying() {
		return false;
	}

	default void setAllowFlight(boolean allow) {
	}

	default void setFlying(boolean flying) {
	}

	default Optional<Inventory> getInventory() {
		if (getEntity() instanceof InventoryHolder) return Optional.of(((InventoryHolder) getEntity()).getInventory());
		return Optional.empty();
	}

	default void validateSlots() {
		IntStream.rangeClosed(1, 9).forEach(i -> getSlotAbility(i).ifPresent(desc -> {
			if (!hasElement(desc.getElement()) || !hasPermission(desc)) setSlotAbilityInternal(i, null);
		}));
	}

	default void sendMessageKyori(String message) {
	}

	default void sendMessageKyori(TextComponent message) {
	}
}
