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

package me.moros.bending.model.user;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import me.moros.bending.event.EventBus;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.board.Board;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.preset.PresetCreateResult;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.profile.BenderData;
import me.moros.bending.model.user.profile.PlayerProfile;
import me.moros.bending.platform.entity.player.GameMode;
import me.moros.bending.platform.entity.player.Player;
import me.moros.bending.platform.property.BooleanProperty;
import me.moros.bending.util.Tasker;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * {@link User} implementation for players.
 */
public final class BendingPlayer extends BendingUser implements PresetUser {
  private final Set<Preset> presets;
  private final int internalId;

  private Board board = Board.dummy();

  private BendingPlayer(Game game, Player player, PlayerProfile profile) {
    super(game, player, profile.benderData());
    this.internalId = profile.id();
    presets = new HashSet<>(profile.benderData().presets());
    if (profile.board()) {
      Tasker.sync().submit(this::board);
    }
  }

  @Override
  public Player entity() {
    return (Player) super.entity();
  }

  @Override
  public TriState checkProperty(BooleanProperty property) {
    return entity().checkProperty(property);
  }

  @Override
  public void setProperty(BooleanProperty property, boolean value) {
    entity().setProperty(property, value);
  }

  @Override
  public boolean isSpectator() {
    return entity().gamemode() == GameMode.SPECTATOR;
  }

  @Override
  public int currentSlot() {
    return entity().inventory().selectedSlot() + 1;
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
    if (!game().worldManager().isEnabled(worldKey()) || store().has(Board.HIDDEN)) {
      board.disableScoreboard();
      board = Board.dummy();
    } else if (!board.isEnabled()) {
      board = Board.create(this);
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
    if (n.isEmpty() || !EventBus.INSTANCE.postPresetCreateEvent(this, preset)) {
      return CompletableFuture.completedFuture(PresetCreateResult.CANCELLED);
    }
    return game().storage().savePresetAsync(internalId, preset).thenApply(success -> {
      if (success) {
        presets.add(preset);
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
    game().storage().deletePresetAsync(preset.id());
    return true;
  }

  public PlayerProfile toProfile() {
    BenderData data = new BenderData(createPresetFromSlots("").abilities(), elements(), presets);
    boolean board = !store().has(Board.HIDDEN);
    return new PlayerProfile(internalId, board, data);
  }

  public static Optional<User> createUser(Game game, Player player, PlayerProfile profile) {
    Objects.requireNonNull(game);
    if (Registries.BENDERS.containsKey(player.uuid())) {
      return Optional.empty();
    }
    return Optional.of(new BendingPlayer(game, player, profile));
  }
}
