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

package me.moros.bending.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.moros.bending.game.Game;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.Attributes;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.ChatUtil;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

@CommandAlias("%modifycommand")
@CommandPermission("bending.command.modify")
public class ModifyCommand extends BaseCommand {
	@HelpCommand
	@CommandPermission("bending.command.help")
	public static void doHelp(CommandSender sender, CommandHelp help) {
		ChatUtil.sendMessage(sender, ChatUtil.brand("Help"));
		help.showHelp();
	}

	@Subcommand("add|a")
	@CommandCompletion("@elements|@abilities @attributes @players")
	@Description("Add a new modifier to the specified player")
	public static void onAdd(BendingPlayer player, ModifyPolicy policy, String type, ModifierOperation operation, double amount, @Optional OnlinePlayer target) {
		String validType = Arrays.stream(Attributes.TYPES)
			.filter(attr -> attr.equalsIgnoreCase(type))
			.findAny().orElseThrow(() -> new InvalidCommandArgument("Invalid attribute type"));
		AttributeModifier modifier = new AttributeModifier(validType, operation, amount);
		BendingPlayer bendingPlayer = target == null ? player : Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		Game.getAttributeSystem().addModifier(bendingPlayer, modifier, policy);
		Game.getAttributeSystem().recalculate(bendingPlayer);
		player.sendMessageKyori(
			TextComponent.of("Successfully added modifier to " + bendingPlayer.getEntity().getName(), NamedTextColor.GREEN)
		);
	}

	@Subcommand("clear|c")
	@CommandCompletion("@players")
	@Description("Clear all existing modifiers for a player")
	public static void onClear(BendingPlayer player, @Optional OnlinePlayer target) {
		BendingPlayer bendingPlayer = target == null ? player : Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		Game.getAttributeSystem().clearModifiers(bendingPlayer);
		Game.getAttributeSystem().recalculate(bendingPlayer);
		player.sendMessageKyori(
			TextComponent.of("Cleared attribute modifiers for " + bendingPlayer.getEntity().getName(), NamedTextColor.GREEN)
		);
	}
}
