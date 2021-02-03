/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.atlas.cf.common.value.qual.IntRange;
import me.moros.atlas.expiringmap.ExpirationPolicy;
import me.moros.atlas.expiringmap.ExpiringMap;
import me.moros.bending.Bending;
import me.moros.bending.events.BindChangeEvent;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.predicate.general.BendingConditions;
import me.moros.bending.model.predicate.general.CompositeBendingConditional;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.slots.AbilitySlotContainer;
import me.moros.bending.util.Tasker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class BendingUser extends CommandUserWrapper implements User {
	private final ElementHolder elementHolder = new ElementHolder();
	private final AbilitySlotContainer slotContainer;
	private final ExpiringMap<AbilityDescription, Boolean> cooldowns = ExpiringMap.builder().variableExpiration().build();
	private final CompositeBendingConditional bendingConditional;
	private final LivingEntity entity;

	protected BendingUser(@NonNull LivingEntity entity) {
		super(entity);
		this.entity = entity;
		cooldowns.addExpirationListener((key, value) ->
			Tasker.newChain().delay(1).execute(() -> Bending.getEventBus().postCooldownRemoveEvent(this, key)));
		slotContainer = new AbilitySlotContainer();
		bendingConditional = BendingConditions.builder().build();
	}

	@Override
	public @NonNull LivingEntity getEntity() {
		return entity;
	}

	@Override
	public @NonNull ElementHolder getElementHolder() {
		return elementHolder;
	}

	public @NonNull Preset createPresetFromSlots(String name) {
		return slotContainer.toPreset(name);
	}

	public int bindPreset(@NonNull Preset preset) {
		slotContainer.fromPreset(preset);
		validateSlots();
		if (this instanceof BendingPlayer) Bending.getGame().getBoardManager().updateBoard((Player) getEntity());
		Bending.getEventBus().postBindChangeEvent(this, BindChangeEvent.Result.MULTIPLE);
		return preset.compare(createPresetFromSlots(""));
	}

	@Override
	public Optional<AbilityDescription> getSlotAbility(@IntRange(from = 1, to = 9) int slot) {
		return Optional.ofNullable(slotContainer.getAbility(slot));
	}

	@Override
	public void setSlotAbilityInternal(@IntRange(from = 1, to = 9) int slot, @Nullable AbilityDescription desc) {
		slotContainer.setAbility(slot, desc);
	}

	@Override
	public void setSlotAbility(@IntRange(from = 1, to = 9) int slot, @Nullable AbilityDescription desc) {
		setSlotAbilityInternal(slot, desc);
		if (this instanceof BendingPlayer)
			Bending.getGame().getBoardManager().updateBoardSlot((Player) getEntity(), desc);
		Bending.getEventBus().postBindChangeEvent(this, BindChangeEvent.Result.SINGLE);
	}

	@Override
	public Optional<AbilityDescription> getSelectedAbility() {
		return Optional.empty(); // Non-player bending users don't have anything selected.
	}

	@Override
	public boolean isOnCooldown(@NonNull AbilityDescription desc) {
		return cooldowns.containsKey(desc);
	}

	@Override
	public void setCooldown(@NonNull AbilityDescription desc, long duration) {
		if (duration <= 0) return;
		if (!isOnCooldown(desc)) {
			cooldowns.put(desc, false, ExpirationPolicy.CREATED, duration, TimeUnit.MILLISECONDS);
			Bending.getEventBus().postCooldownAddEvent(this, desc, duration);
		} else if (duration > cooldowns.getExpectedExpiration(desc)) {
			cooldowns.setExpiration(desc, duration, TimeUnit.MILLISECONDS);
			Bending.getEventBus().postCooldownAddEvent(this, desc, duration);
		}
	}

	@Override
	public @NonNull CompositeBendingConditional getBendingConditional() {
		return bendingConditional;
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

	public static Optional<User> createUser(@NonNull LivingEntity entity) {
		if (entity instanceof Player) return Optional.empty();
		if (Bending.getGame().getBenderRegistry().isBender(entity)) {
			return Bending.getGame().getBenderRegistry().getBendingUser(entity);
		}
		return Optional.of(new BendingUser(entity));
	}
}
