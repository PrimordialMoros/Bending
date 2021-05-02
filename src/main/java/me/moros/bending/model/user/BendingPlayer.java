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

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.preset.PresetCreateResult;
import me.moros.bending.model.user.profile.BendingProfile;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class BendingPlayer extends BendingUser {
  private final BendingProfile profile;
  private final PresetHolder presetHolder;

  private BendingPlayer(Player player, BendingProfile profile) {
    super(player);
    this.profile = profile;
    presetHolder = new PresetHolder(profile.id(), profile.data().presets());
  }

  public @NonNull BendingProfile profile() {
    return profile;
  }

  @Override
  public @NonNull Player entity() {
    return (Player) super.entity();
  }

  /**
   * @return a slot index in the 1-9 range (inclusive)
   */
  public int currentSlot() {
    return entity().getInventory().getHeldItemSlot() + 1;
  }

  @Override
  public Optional<AbilityDescription> selectedAbility() {
    return slotAbility(currentSlot());
  }

  @Override
  public boolean valid() {
    return entity().isOnline();
  }

  @Override
  public boolean spectator() {
    return entity().getGameMode() == GameMode.SPECTATOR;
  }

  @Override
  public boolean sneaking() {
    return entity().isSneaking();
  }

  @Override
  public boolean allowFlight() {
    return entity().getAllowFlight();
  }

  @Override
  public boolean flying() {
    return entity().isFlying();
  }

  @Override
  public void allowFlight(boolean allow) {
    entity().setAllowFlight(allow);
  }

  @Override
  public void flying(boolean flying) {
    entity().setFlying(flying);
  }

  // Presets
  public @NonNull Set<@NonNull String> presets() {
    return presetHolder.presets();
  }

  public Optional<Preset> presetByName(@NonNull String name) {
    return Optional.ofNullable(presetHolder.presetByName(name.toLowerCase()));
  }

  public void addPreset(@NonNull Preset preset, @NonNull Consumer<PresetCreateResult> consumer) {
    String name = preset.name().toLowerCase();
    if (preset.id() > 0 || presetHolder.hasPreset(name)) {
      consumer.accept(PresetCreateResult.EXISTS);
      return;
    }
    presetHolder.addPreset(name);
    Bending.game().storage().savePresetAsync(profile.id(), preset, result ->
      consumer.accept(result ? PresetCreateResult.SUCCESS : PresetCreateResult.FAIL)
    );
  }

  public boolean removePreset(@NonNull Preset preset) {
    String name = preset.name().toLowerCase();
    if (preset.id() <= 0 || !presetHolder.hasPreset(name)) {
      return false;
    }
    Bending.game().storage().deletePresetAsync(preset.id());
    return presetHolder.removePreset(name);
  }

  @Override
  public boolean hasPermission(@NonNull String permission) {
    return entity().hasPermission(permission);
  }

  public static Optional<BendingPlayer> createPlayer(@NonNull Player player, @NonNull BendingProfile profile) {
    if (Bending.game().playerManager().playerExists(player.getUniqueId())) {
      return Optional.empty();
    }
    BendingPlayer bendingPlayer = new BendingPlayer(player, profile);
    return Optional.of(bendingPlayer);
  }
}
