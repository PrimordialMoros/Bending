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

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.moros.bending.Bending;
import me.moros.bending.locale.Message;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.bukkit.command.CommandSender;

@CommandAlias("%modifycommand")
@CommandPermission("bending.command.modify")
public class ModifyCommand extends BaseCommand {
  @HelpCommand
  @CommandPermission("bending.command.help")
  public static void doHelp(CommandSender user, CommandHelp help) {
    user.sendMessage(Message.HELP_HEADER.build());
    help.showHelp();
  }

  @Subcommand("add|a")
  @CommandCompletion("@elements|@allabilities * * * @players")
  @Description("Add a new modifier to the specified player")
  public static void onAdd(BendingPlayer player, ModifyPolicy policy, Attribute attribute, ModifierOperation op, double amount, @Optional @CommandPermission("bending.command.modify.others") OnlinePlayer target) {
    BendingPlayer bendingPlayer = target == null ? player : Registries.BENDERS.user(target.getPlayer());
    AttributeModifier modifier = new AttributeModifier(policy, attribute, op, amount);
    Registries.ATTRIBUTES.add(bendingPlayer, modifier);
    recalculate(bendingPlayer);
    Message.MODIFIER_ADD.send(bendingPlayer, bendingPlayer.entity().getName());
  }

  @Subcommand("clear|c")
  @CommandCompletion("@players")
  @Description("Clear all existing modifiers for a player")
  public static void onClear(BendingPlayer player, @Optional @CommandPermission("bending.command.modify.others") OnlinePlayer target) {
    BendingPlayer bendingPlayer = target == null ? player : Registries.BENDERS.user(target.getPlayer());
    Registries.ATTRIBUTES.invalidate(bendingPlayer.entity().getUniqueId());
    recalculate(bendingPlayer);
    Message.MODIFIER_CLEAR.send(bendingPlayer, bendingPlayer.entity().getName());
  }

  private static void recalculate(User user) {
    Bending.game().abilityManager(user.world()).userInstances(user).forEach(Ability::loadConfig);
  }
}
