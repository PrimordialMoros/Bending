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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import me.moros.atlas.caffeine.cache.AsyncLoadingCache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.preset.PresetCreateResult;
import me.moros.bending.model.user.profile.BendingProfile;
import me.moros.bending.registry.BenderRegistry;
import me.moros.bending.registry.Registries;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BendingPlayer extends BendingUser implements PresetUser {
  private final BendingProfile profile;
  private final Set<String> presets;
  private final AsyncLoadingCache<String, Preset> presetCache;

  private BendingPlayer(Player player, BendingProfile profile) {
    super(player, profile.data());
    this.profile = profile;
    presets = new HashSet<>(profile.data().presets());
    presetCache = Caffeine.newBuilder()
      .maximumSize(8) // Average player will probably have 2-5 presets, this should be enough
      .buildAsync(this::loadPreset);
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
  @Override
  public @NonNull Set<@NonNull String> presets() {
    return Set.copyOf(presets);
  }

  @Override
  public Optional<Preset> presetByName(@NonNull String name) {
    if (!presets.contains(name.toLowerCase())) {
      return Optional.empty();
    }
    return Optional.ofNullable(presetCache.synchronous().get(name.toLowerCase()));
  }

  @Override
  public @NonNull CompletableFuture<@NonNull PresetCreateResult> addPreset(@NonNull Preset preset) {
    String name = preset.name().toLowerCase();
    if (preset.id() > 0 || presets.contains(name)) {
      return CompletableFuture.completedFuture(PresetCreateResult.EXISTS);
    }
    if (!Bending.eventBus().postPresetCreateEvent(this, preset)) {
      return CompletableFuture.completedFuture(PresetCreateResult.CANCELLED);
    }
    presets.add(name);
    return Bending.game().storage().savePresetAsync(profile.id(), preset).thenApply(result ->
      result ? PresetCreateResult.SUCCESS : PresetCreateResult.FAIL
    );
  }

  @Override
  public boolean removePreset(@NonNull Preset preset) {
    String name = preset.name().toLowerCase();
    if (preset.id() <= 0 || !presets.contains(name)) {
      return false;
    }
    Bending.game().storage().deletePresetAsync(preset.id());
    return presets.remove(name);
  }

  private Preset loadPreset(String name) {
    if (!presets.contains(name.toLowerCase())) {
      return null;
    }
    return Bending.game().storage().loadPreset(profile().id(), name);
  }

  /**
   * Use {@link BenderRegistry#register(Player, BendingProfile)}
   */
  public static Optional<BendingPlayer> createUser(@NonNull Player player, @NonNull BendingProfile profile) {
    if (Registries.BENDERS.contains(player.getUniqueId())) {
      return Optional.empty();
    }
    BendingPlayer bendingPlayer = new BendingPlayer(player, profile);
    return Optional.of(bendingPlayer);
  }
}
