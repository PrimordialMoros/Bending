/*
 * Copyright 2020-2021 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.user;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.preset.PresetCreateResult;
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.registry.Registries;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BendingPlayer extends BendingUser implements PresetUser {
  private final Set<Preset> presets;
  private final int internalId;
  private boolean board;

  private BendingPlayer(Player player, PlayerProfile profile) {
    super(player, profile.benderData());
    this.internalId = profile.id();
    this.board = profile.board();
    presets = new HashSet<>(profile.benderData().presets());
  }

  public boolean board() {
    return board;
  }

  public void board(boolean value) {
    this.board = value;
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
  public @Nullable AbilityDescription selectedAbility() {
    return boundAbility(currentSlot());
  }

  @Override
  public boolean hasPermission(@NonNull String permission) {
    return entity().hasPermission(permission);
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
  public boolean sprinting() {
    return entity().isSprinting();
  }

  @Override
  public void sprinting(boolean sprinting) {
    if (sprinting() != sprinting) {
      entity().setSprinting(sprinting);
    }
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

  @Override
  public @NonNull PlayerInventory inventory() {
    return entity().getInventory();
  }

  // Presets
  @Override
  public @NonNull Set<@NonNull Preset> presets() {
    return Set.copyOf(presets);
  }

  @Override
  public @Nullable Preset presetByName(@NonNull String name) {
    return presets.stream().filter(p -> p.name().equalsIgnoreCase(name)).findAny().orElse(null);
  }

  @Override
  public @NonNull CompletableFuture<@NonNull PresetCreateResult> addPreset(@NonNull Preset preset) {
    if (preset.id() > 0 || presets.contains(preset)) {
      return CompletableFuture.completedFuture(PresetCreateResult.EXISTS);
    }
    if (!Bending.eventBus().postPresetCreateEvent(this, preset)) {
      return CompletableFuture.completedFuture(PresetCreateResult.CANCELLED);
    }
    presets.add(preset);
    return Bending.game().storage().savePresetAsync(internalId, preset).thenApply(result ->
      result ? PresetCreateResult.SUCCESS : PresetCreateResult.FAIL
    );
  }

  @Override
  public boolean removePreset(@NonNull Preset preset) {
    if (preset.id() <= 0 || !presets.contains(preset)) {
      return false;
    }
    Bending.game().storage().deletePresetAsync(preset.id());
    return presets.remove(preset);
  }

  public @NonNull PlayerProfile toProfile() {
    BenderData data = new BenderData(createPresetFromSlots("").abilities(), elements(), presets);
    return new PlayerProfile(internalId, board, data);
  }

  public static Optional<BendingPlayer> createUser(@NonNull Player player, @NonNull PlayerProfile profile) {
    if (Registries.BENDERS.contains(player.getUniqueId())) {
      return Optional.empty();
    }
    BendingPlayer bendingPlayer = new BendingPlayer(player, profile);
    return Optional.of(bendingPlayer);
  }
}
