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
import me.moros.bending.events.BindChangeEvent;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.predicates.conditionals.CompositeBendingConditional;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.slots.AbilitySlotContainer;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.Tasker;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class BendingUser implements User {
	private final ElementHolder elementHolder = new ElementHolder();
	private final ArrayDeque<AbilitySlotContainer> slotContainers = new ArrayDeque<>(2);
	private final ExpiringMap<AbilityDescription, Boolean> cooldowns = ExpiringMap.builder().variableExpiration().build();
	private final CompositeBendingConditional bendingConditional;
	private final LivingEntity entity;

	protected BendingUser(LivingEntity entity) {
		this.entity = entity;
		cooldowns.addExpirationListener((key, value) ->
			Tasker.newChain().delay(1).execute(() -> Bending.getEventBus().postCooldownRemoveEvent(this, key)));
		slotContainers.add(new AbilitySlotContainer(9));
		bendingConditional = CompositeBendingConditional.defaults().build();
	}

	@Override
	public LivingEntity getEntity() {
		return entity;
	}

	@Override
	public ElementHolder getElementHolder() {
		return elementHolder;
	}

	public Preset createPresetFromSlots(String name) {
		return slotContainers.getFirst().toPreset(name);
	}

	public int bindPreset(Preset preset) {
		slotContainers.getFirst().fromPreset(preset);
		validateSlots();
		if (this instanceof BendingPlayer) Game.getBoardManager().updateBoard((Player) getEntity());
		Bending.getEventBus().postBindChangeEvent(this, BindChangeEvent.Result.MULTIPLE);
		return preset.compare(createPresetFromSlots(""));
	}

	public Optional<AbilityDescription> getStandardSlotAbility(int slot) {
		return Optional.ofNullable(slotContainers.getFirst().getAbility(slot));
	}

	@Override
	public Optional<AbilityDescription> getSlotAbility(int slot) {
		return Optional.ofNullable(slotContainers.getLast().getAbility(slot));
	}

	@Override
	public void setSlotAbilityInternal(int slot, AbilityDescription desc) {
		slotContainers.getFirst().setAbility(slot, desc);
	}

	@Override
	public void setSlotAbility(int slot, AbilityDescription desc) {
		setSlotAbilityInternal(slot, desc);
		if (this instanceof BendingPlayer) Game.getBoardManager().updateBoardSlot((Player) getEntity(), desc);
		Bending.getEventBus().postBindChangeEvent(this, BindChangeEvent.Result.SINGLE);
	}

	@Override
	public Optional<AbilityDescription> getSelectedAbility() {
		return Optional.empty(); // Non-player bending users don't have anything selected.
	}

	// Adds or replaces the last container
	@Override
	public void addSlotContainer(AbilitySlotContainer slotContainer) {
		removeLastSlotContainer();
		slotContainers.addLast(slotContainer);
	}

	// Removes any multi ability containers while keeping the original standard slot container
	@Override
	public void removeLastSlotContainer() {
		if (slotContainers.size() > 1) slotContainers.pollLast();
	}

	@Override
	public boolean isOnCooldown(AbilityDescription desc) {
		return cooldowns.containsKey(desc);
	}

	@Override
	public void setCooldown(AbilityDescription desc, long duration) {
		if (duration <= 0) return;
		if (!isOnCooldown(desc)) {
			cooldowns.put(desc, false, ExpirationPolicy.CREATED, duration, TimeUnit.MILLISECONDS);
			Bending.getEventBus().postCooldownAddEvent(this, desc);
		} else if (duration > cooldowns.getExpectedExpiration(desc)) {
			cooldowns.setExpiration(desc, duration, TimeUnit.MILLISECONDS);
			Bending.getEventBus().postCooldownAddEvent(this, desc);
		}
	}

	@Override
	public boolean canBend(AbilityDescription desc) {
		return bendingConditional.canBend(this, desc);
	}

	@Override
	public CompositeBendingConditional getBendingConditional() {
		return bendingConditional;
	}

	@Override
	public boolean hasPermission(String permission) {
		return true;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BendingUser) {
			return getEntity().equals(((BendingUser) obj).getEntity());
		}
		return getEntity().equals(obj);
	}

	@Override
	public int hashCode() {
		return entity.hashCode();
	}
}
