/*
 * Copyright 2020-2025 Moros
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

import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.moros.bending.api.game.Game;
import me.moros.bending.api.gui.Board;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.DelegatePlayer;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.entity.player.GameMode;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.util.FeaturePermissions;
import net.kyori.adventure.util.TriState;

/**
 * {@link User} implementation for players.
 */
final class BendingPlayer extends BendingUser implements DelegatePlayer {
  private final Cache<String, Boolean> permissionCache;

  private Board board;

  BendingPlayer(Game game, Player player) {
    super(game, player);
    this.permissionCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();
    this.board = Board.dummy();
  }

  @Override
  public Player entity() {
    return (Player) super.entity();
  }

  @Override
  public boolean isSpectator() {
    return propertyValue(EntityProperties.GAMEMODE) == GameMode.SPECTATOR;
  }

  @Override
  public int currentSlot() {
    return inventory().selectedSlot() + 1;
  }

  @Override
  public void currentSlot(int slot) {
  }

  @Override
  public boolean hasPermission(String permission) {
    return permissionCache.get(permission, entity()::hasPermission);
  }

  @Override
  public TriState setPermission(String permission, TriState state) {
    return TriState.NOT_SET;
  }

  @Override
  public Board board() {
    if (!canUseBoard()) {
      if (board.isEnabled()) {
        board.disableScoreboard();
        board = Board.dummy();
      }
    } else if (!board.isEnabled()) {
      board = Platform.instance().factory().buildBoard(this).orElseGet(Board::dummy);
    }
    return board;
  }

  private boolean canUseBoard() {
    if (store().has(Board.HIDDEN) || !game().worldManager().isEnabled(worldKey()) || !canBend()) {
      return false;
    }
    return hasElements() && hasPermission(FeaturePermissions.BOARD);
  }
}
