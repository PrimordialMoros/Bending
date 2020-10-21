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
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.preset.PresetCreateResult;
import me.moros.bending.model.user.profile.BendingProfile;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class BendingPlayer extends BendingUser {
	private final BendingProfile profile;
	private final PresetHolder presetHolder;

	private BendingPlayer(Player player, BendingProfile profile, Set<String> presets) {
		super(player);
		this.profile = profile;
		presetHolder = new PresetHolder(profile.getInternalId(), presets);
	}

	public @NonNull BendingProfile getProfile() {
		return profile;
	}

	@Override
	public @NonNull Player getEntity() {
		return (Player) super.getEntity();
	}

	/**
	 * @return a slot index in the 1-9 range (inclusive)
	 */
	public int getHeldItemSlot() {
		return getEntity().getInventory().getHeldItemSlot() + 1;
	}

	@Override
	public Optional<AbilityDescription> getSelectedAbility() {
		return getSlotAbility(getHeldItemSlot());
	}

	@Override
	public boolean isValid() {
		return getEntity().isOnline();
	}

	@Override
	public boolean isSpectator() {
		return getEntity().getGameMode() == GameMode.SPECTATOR;
	}

	@Override
	public boolean isSneaking() {
		return getEntity().isSneaking();
	}

	@Override
	public boolean getAllowFlight() {
		return getEntity().getAllowFlight();
	}

	@Override
	public boolean isFlying() {
		return getEntity().isFlying();
	}

	@Override
	public void setAllowFlight(boolean allow) {
		getEntity().setAllowFlight(allow);
	}

	@Override
	public void setFlying(boolean flying) {
		getEntity().setFlying(flying);
	}

	// Presets
	public @NonNull Set<@NonNull String> getPresets() {
		return presetHolder.getPresets();
	}

	public Optional<Preset> getPresetByName(@NonNull String name) {
		return Optional.ofNullable(presetHolder.getPresetByName(name.toLowerCase()));
	}

	public void addPreset(@NonNull Preset preset, @NonNull Consumer<PresetCreateResult> consumer) {
		String name = preset.getName().toLowerCase();
		if (preset.getInternalId() > 0 || presetHolder.hasPreset(name)) {
			consumer.accept(PresetCreateResult.EXISTS);
			return;
		}
		presetHolder.addPreset(name);
		Bending.getGame().getStorage().savePresetAsync(profile.getInternalId(), preset, result ->
			consumer.accept(result ? PresetCreateResult.SUCCESS : PresetCreateResult.FAIL)
		);
	}

	public boolean removePreset(@NonNull Preset preset) {
		String name = preset.getName().toLowerCase();
		if (preset.getInternalId() <= 0 || !presetHolder.hasPreset(name)) {
			return false;
		}
		Bending.getGame().getStorage().deletePresetAsync(preset.getInternalId());
		return presetHolder.removePreset(name);
	}

	@Override
	public boolean hasPermission(@NonNull String permission) {
		return getEntity().hasPermission(permission);
	}

	public static Optional<BendingPlayer> createPlayer(@NonNull Player player, @NonNull BendingProfile profile) {
		if (Bending.getGame().getPlayerManager().playerExists(player.getUniqueId())) return Optional.empty();
		BendingPlayer bendingPlayer = new BendingPlayer(player, profile, new HashSet<>(profile.getData().presets));
		return Optional.of(bendingPlayer);
	}
}
