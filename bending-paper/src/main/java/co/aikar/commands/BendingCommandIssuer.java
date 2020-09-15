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

package co.aikar.commands;

import me.moros.bending.game.Game;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.ChatUtil;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BendingCommandIssuer extends BukkitCommandIssuer {
	BendingCommandIssuer(BukkitCommandManager manager, CommandSender sender) {
		super(manager, sender);
	}

	public void sendMessageKyori(String message) {
		ChatUtil.sendMessage(getBendingPlayer(), message);
	}

	public void sendMessageKyori(TextComponent message) {
		ChatUtil.sendMessage(getBendingPlayer(), message);
	}

	public BendingPlayer getBendingPlayer() {
		if (!(getIssuer() instanceof Player)) {
			throw new IllegalArgumentException("You need to be a player to execute this command.");
		}
		return Game.getPlayerManager().getPlayer(getPlayer().getUniqueId());
	}
}
