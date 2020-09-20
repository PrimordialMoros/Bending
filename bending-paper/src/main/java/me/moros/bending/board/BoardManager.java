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

package me.moros.bending.board;

import me.moros.bending.config.Configurable;
import me.moros.bending.game.Game;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.util.ChatUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages every individual {@link Board}
 */
public final class BoardManager extends Configurable {
	private final Map<UUID, Board> scoreboardPlayers = new HashMap<>();
	private boolean enabled;

	public BoardManager() {
		onConfigReload();
	}

	/**
	 * Force toggle the scoreboard for when a player changes worlds (for example when teleporting to a world where bending is disabled)
	 * @param player the player to force toggle
	 */
	public void forceToggleScoreboard(Player player) {
		if (Game.isDisabledWorld(player.getWorld().getUID())) {
			UUID uuid = player.getUniqueId();
			if (scoreboardPlayers.containsKey(uuid)) {
				scoreboardPlayers.get(uuid).disableScoreboard();
				scoreboardPlayers.remove(uuid);
			}
		} else {
			canUseScoreboard(player);
		}
	}

	public boolean toggleScoreboard(Player player) {
		if (!enabled || Game.isDisabledWorld(player.getWorld().getUID())) {
			return false;
		}
		UUID uuid = player.getUniqueId();
		if (scoreboardPlayers.containsKey(uuid)) {
			scoreboardPlayers.get(uuid).disableScoreboard();
			scoreboardPlayers.remove(uuid);
			return false;
		} else {
			return canUseScoreboard(player);
		}
	}

	/**
	 * Checks if a player can use the bending board and creates a BendingBoardInstance if possible.
	 * @param player the player to check
	 * @return true if player can use the bending board, false otherwise
	 */
	public boolean canUseScoreboard(Player player) {
		if (!enabled || Game.isDisabledWorld(player.getWorld().getUID())) {
			return false;
		}
		UUID uuid = player.getUniqueId();
		if (!scoreboardPlayers.containsKey(uuid)) {
			scoreboardPlayers.put(uuid, new Board(player));
		}
		return true;
	}

	public void updateBoard(Player player) {
		if (canUseScoreboard(player)) {
			scoreboardPlayers.get(player.getUniqueId()).updateAll();
		}
	}

	public void updateBoardSlot(Player player, AbilityDescription desc) {
		updateBoardSlot(player, desc, false);
	}

	public void updateBoardSlot(Player player, AbilityDescription desc, boolean cooldown) {
		if (canUseScoreboard(player)) {
			if (desc != null && desc.isActivatedBy(ActivationMethod.SEQUENCE)) {
				String value = "  " + ChatUtil.getLegacyColor(desc.getElement().getColor()) + ChatColor.STRIKETHROUGH + desc.getName();
				scoreboardPlayers.get(player.getUniqueId()).updateMisc(value, cooldown, true);
			} else {
				scoreboardPlayers.get(player.getUniqueId()).updateAll();
			}
		}
	}

	public void changeActiveSlot(Player player, int oldSlot, int newSlot) {
		if (canUseScoreboard(player)) {
			scoreboardPlayers.get(player.getUniqueId()).setActiveSlot(++oldSlot, ++newSlot);
		}
	}

	public void invalidate(UUID uuid) {
		scoreboardPlayers.remove(uuid);
	}

	@Override
	public void onConfigReload() {
		enabled = config.getNode("properties", "bending-board").getBoolean(true);
	}
}
