/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.api.user;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.ability.preset.PresetCreateResult;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.DelegatePlayer;
import me.moros.bending.api.platform.entity.player.GameMode;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.user.profile.BenderProfile;
import me.moros.bending.api.user.profile.Identifiable;
import me.moros.bending.api.user.profile.PlayerBenderProfile;
import me.moros.bending.api.util.Tasker;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link User} implementation for players.
 */
public final class BendingPlayer extends BendingUser implements PresetUser, DelegatePlayer {
  private final int internalId;
  private final Set<Preset> presets;

  private Board board;

  BendingPlayer(Game game, Player player, PlayerBenderProfile profile) {
    super(game, player, profile);
    this.internalId = profile.id();
    this.presets = new HashSet<>(profile.presets());
    this.board = Board.dummy();
    if (profile.board()) {
      Tasker.sync().submit(this::board);
    }
  }

  @Override
  public Player entity() {
    return (Player) super.entity();
  }

  @Override
  public boolean isSpectator() {
    return gamemode() == GameMode.SPECTATOR;
  }

  @Override
  public int currentSlot() {
    return inventory().selectedSlot() + 1;
  }

  @Override
  public void currentSlot(int slot) {
  }

  @Override
  public @Nullable AbilityDescription selectedAbility() {
    return boundAbility(currentSlot());
  }

  @Override
  public boolean hasPermission(String permission) {
    return entity().hasPermission(permission);
  }

  @Override
  public TriState setPermission(String permission, TriState state) {
    return TriState.NOT_SET;
  }

  @Override
  public Board board() {
    if (!game().worldManager().isEnabled(worldKey()) || !hasPermission("bending.board") || store().has(Board.HIDDEN)) {
      board.disableScoreboard();
      board = Board.dummy();
    } else if (!board.isEnabled()) {
      board = Platform.instance().factory().buildBoard(this).orElseGet(Board::dummy);
    }
    return board;
  }

  // Presets
  @Override
  public Set<Preset> presets() {
    return Set.copyOf(presets);
  }

  @Override
  public @Nullable Preset presetByName(String name) {
    return presets.stream().filter(p -> p.name().equalsIgnoreCase(name)).findAny().orElse(null);
  }

  @Override
  public CompletableFuture<PresetCreateResult> addPreset(Preset preset) {
    String n = preset.name();
    if (preset.id() > 0 || presets.contains(preset) || presets.stream().map(Preset::name).anyMatch(n::equalsIgnoreCase)) {
      return CompletableFuture.completedFuture(PresetCreateResult.EXISTS);
    }
    if (n.isEmpty() || !game().eventBus().postPresetCreateEvent(this, preset)) {
      return CompletableFuture.completedFuture(PresetCreateResult.CANCELLED);
    }
    return game().storage().savePresetAsync(Identifiable.of(internalId, uuid()), preset).thenApply(id -> {
      if (id > 0) {
        presets.add(preset.withId(id));
        return PresetCreateResult.SUCCESS;
      }
      return PresetCreateResult.FAIL;
    });
  }

  @Override
  public boolean removePreset(Preset preset) {
    if (preset.id() <= 0 || !presets.contains(preset)) {
      return false;
    }
    presets.remove(preset);
    game().storage().deletePresetAsync(Identifiable.of(internalId, uuid()), preset);
    return true;
  }

  public PlayerBenderProfile toProfile() {
    var data = BenderProfile.of(createPresetFromSlots("").abilities(), elements(), presets);
    return BenderProfile.of(internalId, uuid(), !store().has(Board.HIDDEN), data);
  }
}
