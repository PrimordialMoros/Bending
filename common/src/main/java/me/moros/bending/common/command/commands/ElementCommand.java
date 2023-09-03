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

package me.moros.bending.common.command.commands;

import java.util.function.BiConsumer;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.meta.CommandMeta;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.CommandUtil;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.command.argument.UserArgument;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;

public record ElementCommand<C extends Audience>(Commander<C> commander) implements Initializer, ElementHandler {
  @Override
  public void init() {
    Builder<C> builder = commander().rootBuilder();
    commander().register(builder.literal("choose", "ch")
      .meta(CommandMeta.DESCRIPTION, "Choose an element through the GUI")
      .permission(CommandPermissions.CHOOSE)
      .senderType(commander().playerType())
      .handler(c -> onElementChooseGUI(c.get(ContextKeys.BENDING_PLAYER)))
    );

    commander().manager().command(builder.literal("choose", "ch")
      .meta(CommandMeta.DESCRIPTION, "Choose an element")
      .permission(CommandPermissions.CHOOSE)
      .senderType(commander().playerType())
      .argument(EnumArgument.of(Element.class, "element"))
      .handler(c -> onElementChoose(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
    );
    commander().manager().command(builder.literal("choose", "ch")
      .meta(CommandMeta.DESCRIPTION, "Choose an element for a specific user")
      .permission(CommandPermissions.CHOOSE + ".other")
      .argument(EnumArgument.of(Element.class, "element"))
      .argument(UserArgument.of("target"))
      .handler(c -> onElementSet(c.get("target"), c.get("element")))
    );

    dualRegister(builder.literal("add", "a")
      .meta(CommandMeta.DESCRIPTION, "Add an element").permission(CommandPermissions.ADD), this::onElementAdd);
    dualRegister(builder.literal("remove", "rm")
      .meta(CommandMeta.DESCRIPTION, "Remove an element").permission(CommandPermissions.REMOVE), this::onElementRemove);
  }

  private void dualRegister(Builder<C> builder, BiConsumer<User, Element> handler) {
    commander().manager().command(builder
      .senderType(commander().playerType())
      .argument(EnumArgument.of(Element.class, "element"))
      .handler(c -> handler.accept(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
    );
    commander().manager().command(builder
      .permission(builder.commandPermission() + ".other")
      .argument(EnumArgument.of(Element.class, "element"))
      .argument(UserArgument.of("target"))
      .handler(c -> handler.accept(c.get("target"), c.get("element")))
    );
  }

  private void onElementChooseGUI(User user) {
    Platform.instance().factory().buildMenu(this, user).ifPresent(g -> g.show((Player) user));
  }

  @Override
  public void onElementChoose(User user, Element element) {
    if (!user.hasPermission(CommandPermissions.CHOOSE + "." + element.key().value())) {
      Message.ELEMENT_CHOOSE_NO_PERMISSION.send(user, element.displayName());
      return;
    }
    onElementSet(user, element);
  }

  private void onElementSet(User user, Element element) {
    if (user.chooseElement(element)) {
      Message.ELEMENT_CHOOSE_SUCCESS.send(user, element.displayName());
      sendElementNotification(user, element);
    } else {
      Message.ELEMENT_CHOOSE_FAIL.send(user, element.displayName());
    }
  }

  @Override
  public void onElementAdd(User user, Element element) {
    if (user.addElement(element)) {
      Message.ELEMENT_ADD_SUCCESS.send(user, element.displayName());
      sendElementNotification(user, element);
    } else {
      Message.ELEMENT_ADD_FAIL.send(user, element.displayName());
    }
  }

  @Override
  public void onElementRemove(User user, Element element) {
    if (user.removeElement(element)) {
      Message.ELEMENT_REMOVE_SUCCESS.send(user, element.displayName());
    } else {
      Message.ELEMENT_REMOVE_FAIL.send(user, element.displayName());
    }
  }

  @Override
  public void onElementDisplay(User user, Element element) {
    var display = CommandUtil.collectAll(user::hasPermission, element);
    if (display.isEmpty()) {
      Message.ELEMENT_ABILITIES_EMPTY.send(user, element.displayName());
    } else {
      Message.ELEMENT_ABILITIES_HEADER.send(user, element.displayName(), element.description());
      display.forEach(user::sendMessage);
    }
  }

  private void sendElementNotification(User user, Element element) {
    if (user instanceof Player player) {
      player.sendNotification(Item.NETHER_STAR, Message.ELEMENT_TOAST_NOTIFICATION.build(element.displayName()));
    }
  }
}
