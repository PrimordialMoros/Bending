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
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Optional;
import me.moros.bending.gui.ElementMenu;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%elementcommand")
public class ElementCommand extends BaseCommand {
  @Default
  @CommandPermission("bending.command.choose")
  @CommandCompletion("@elements")
  @Description("Choose an element")
  public static void onElementChoose(Player user, @Optional Element element) {
    if (element == null) {
      new ElementMenu(user);
    } else {
      BendingCommand.onElementChoose(user, element, null);
    }
  }

  @HelpCommand
  @CommandPermission("bending.command.help")
  public static void doHelp(CommandSender user, CommandHelp help) {
    Message.HELP_HEADER.send(user);
    help.showHelp();
  }

  public static void onElementChoose(CommandSender user, Element element) {
    BendingCommand.onElementChoose(user, element, null);
  }

  public static void onElementAdd(CommandSender user, Element element) {
    BendingCommand.onElementAdd(user, element, null);
  }

  public static void onElementRemove(CommandSender user, Element element) {
    BendingCommand.onElementRemove(user, element, null);
  }

  public static void onElementDisplay(CommandSender user, Element element) {
    BendingCommand.onDisplay(user, element);
  }
}
