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

import java.util.Collection;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import me.moros.bending.Bending;
import me.moros.bending.gui.ElementMenu;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ElementCommand {
  ElementCommand(PaperCommandManager<CommandSender> manager) {
    Builder<CommandSender> builder = manager.commandBuilder("element", "elements", "elem", "ele")
      .meta(CommandMeta.DESCRIPTION, "Base command for bending element menu");
    manager
      .command(builder
        .meta(CommandMeta.DESCRIPTION, "Choose an element")
        .permission("bending.command.choose")
        .senderType(Player.class)
        .argument(EnumArgument.optional(Element.class, "element"))
        .handler(c -> onBase(c.get(ContextKeys.BENDING_PLAYER), c.getOrDefault("element", null)))
      );
  }

  private static void onBase(BendingPlayer player, Element element) {
    if (element == null) {
      new ElementMenu(player.entity());
    } else {
      onElementChoose(player, element);
    }
  }

  public static void onElementChoose(User user, Element element) {
    if (!user.hasPermission("bending.command.choose." + element)) {
      Message.ELEMENT_CHOOSE_NO_PERMISSION.send(user, element.displayName());
      return;
    }
    if (user.chooseElement(element)) {
      Message.ELEMENT_CHOOSE_SUCCESS.send(user, element.displayName());
    } else {
      Message.ELEMENT_CHOOSE_FAIL.send(user, element.displayName());
    }
  }

  public static void onElementAdd(User user, Element element) {
    if (!user.hasPermission("bending.command.add." + element)) {
      Message.ELEMENT_ADD_NO_PERMISSION.send(user, element.displayName());
      return;
    }
    if (user.addElement(element)) {
      Bending.game().abilityManager(user.world()).createPassives(user);
      Message.ELEMENT_ADD_SUCCESS.send(user, element.displayName());
    } else {
      Message.ELEMENT_ADD_FAIL.send(user, element.displayName());
    }
  }

  public static void onElementRemove(User user, Element element) {
    if (user.removeElement(element)) {
      Bending.game().abilityManager(user.world()).createPassives(user);
      Message.ELEMENT_REMOVE_SUCCESS.send(user, element.displayName());
    } else {
      Message.ELEMENT_REMOVE_FAIL.send(user, element.displayName());
    }
  }

  public static void onElementDisplay(User user, Element element) {
    onElementDisplay(user.entity(), element);
  }

  public static void onElementDisplay(CommandSender user, Element element) {
    Collection<Component> abilities = collectAbilities(user, element);
    Collection<Component> sequences = collectSequences(user, element);
    Collection<Component> passives = collectPassives(user, element);
    if (abilities.isEmpty() && sequences.isEmpty() && passives.isEmpty()) {
      Message.ELEMENT_ABILITIES_EMPTY.send(user, element.displayName());
    } else {
      Message.ELEMENT_ABILITIES_HEADER.send(user, element.displayName(), element.description());
      JoinConfiguration sep = JoinConfiguration.separator(Component.text(", ", NamedTextColor.WHITE));
      if (!abilities.isEmpty()) {
        Message.ABILITIES.send(user);
        user.sendMessage(Component.join(sep, abilities));
      }
      if (!sequences.isEmpty()) {
        Message.SEQUENCES.send(user);
        user.sendMessage(Component.join(sep, sequences));
      }
      if (!passives.isEmpty()) {
        Message.PASSIVES.send(user);
        user.sendMessage(Component.join(sep, passives));
      }
    }
  }

  private static Collection<Component> collectAbilities(CommandSender user, Element element) {
    return Registries.ABILITIES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(desc -> !desc.isActivatedBy(Activation.SEQUENCE) && !desc.isActivatedBy(Activation.PASSIVE))
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .toList();
  }

  private static Collection<Component> collectSequences(CommandSender user, Element element) {
    return Registries.SEQUENCES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .toList();
  }

  private static Collection<Component> collectPassives(CommandSender user, Element element) {
    return Registries.ABILITIES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden() && desc.isActivatedBy(Activation.PASSIVE))
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .toList();
  }
}
