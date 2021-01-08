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

package me.moros.bending.command;

import me.moros.atlas.acf.BaseCommand;
import me.moros.atlas.acf.CommandHelp;
import me.moros.atlas.acf.InvalidCommandArgument;
import me.moros.atlas.acf.annotation.CommandAlias;
import me.moros.atlas.acf.annotation.CommandCompletion;
import me.moros.atlas.acf.annotation.CommandPermission;
import me.moros.atlas.acf.annotation.Description;
import me.moros.atlas.acf.annotation.HelpCommand;
import me.moros.atlas.acf.annotation.Optional;
import me.moros.atlas.acf.annotation.Subcommand;
import me.moros.atlas.acf.bukkit.contexts.OnlinePlayer;
import me.moros.bending.Bending;
import me.moros.bending.locale.Message;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.CommandUser;

import java.util.Arrays;

@CommandAlias("%modifycommand")
@CommandPermission("bending.command.modify")
public class ModifyCommand extends BaseCommand {
	@HelpCommand
	@CommandPermission("bending.command.help")
	public static void doHelp(CommandUser user, CommandHelp help) {
		Message.HELP_HEADER.send(user);
		help.showHelp();
	}

	@Subcommand("add|a")
	@CommandCompletion("@elements|@abilities @attributes @players")
	@Description("Add a new modifier to the specified player")
	public static void onAdd(BendingPlayer player, ModifyPolicy policy, String type, ModifierOperation operation, double amount, @Optional OnlinePlayer target) {
		String validType = Arrays.stream(Attribute.TYPES)
			.filter(attr -> attr.equalsIgnoreCase(type))
			.findAny().orElseThrow(() -> new InvalidCommandArgument("Invalid attribute type"));
		AttributeModifier modifier = new AttributeModifier(validType, operation, amount);
		BendingPlayer bendingPlayer = target == null ? player : Bending.getGame().getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		Bending.getGame().getAttributeSystem().addModifier(bendingPlayer, modifier, policy);
		Bending.getGame().getAttributeSystem().recalculate(bendingPlayer);
		Message.MODIFIER_ADD.send(bendingPlayer, bendingPlayer.getEntity().getName());
	}

	@Subcommand("clear|c")
	@CommandCompletion("@players")
	@Description("Clear all existing modifiers for a player")
	public static void onClear(BendingPlayer player, @Optional OnlinePlayer target) {
		BendingPlayer bendingPlayer = target == null ? player : Bending.getGame().getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		Bending.getGame().getAttributeSystem().clearModifiers(bendingPlayer);
		Bending.getGame().getAttributeSystem().recalculate(bendingPlayer);
		Message.MODIFIER_CLEAR.send(bendingPlayer, bendingPlayer.getEntity().getName());
	}
}
