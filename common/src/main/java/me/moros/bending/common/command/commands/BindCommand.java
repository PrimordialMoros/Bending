/*
 * Copyright 2020-2024 Moros
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

import java.util.Collection;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.command.parser.AbilityDescriptionParser;
import me.moros.bending.common.command.parser.UserParser;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.HoverEvent;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.description.Description;
import org.incendo.cloud.parser.standard.IntegerParser;

public record BindCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    var builder = commander().rootBuilder();
    commander().register(builder
      .literal("bind", "b")
      .required("ability", AbilityDescriptionParser.parser(false))
      .optional("slot", IntegerParser.integerParser(1, 9), DefaultValue.constant(0))
      .commandDescription(Description.of("Bind an ability to a slot"))
      .permission(CommandPermissions.BIND)
      .senderType(commander().playerType())
      .handler(c -> onBind(c.get(ContextKeys.BENDING_PLAYER), c.get("ability"), c.get("slot")))
    );
    commander().register(builder
      .literal("clear", "c")
      .optional("slot", IntegerParser.integerParser(1, 9), DefaultValue.constant(0))
      .commandDescription(Description.of("Clear an ability slot"))
      .permission(CommandPermissions.BIND)
      .senderType(commander().playerType())
      .handler(c -> onBindClear(c.get(ContextKeys.BENDING_PLAYER), c.get("slot")))
    );
    commander().register(builder
      .literal("who", "w")
      .optional("target", UserParser.parser(), DefaultValue.parsed("me"))
      .commandDescription(Description.of("Show all bound abilities"))
      .permission(CommandPermissions.HELP)
      .handler(c -> onBindList(c.sender(), c.get("target")))
    );
  }

  private void onBind(User user, AbilityDescription ability, int slot) {
    if (!ability.canBind()) {
      Message.ABILITY_BIND_FAIL.send(user, ability.displayName());
      return;
    }
    if (!user.hasPermission(ability)) {
      Message.ABILITY_BIND_NO_PERMISSION.send(user, ability.displayName());
    }
    if (!user.hasElement(ability.element())) {
      Message.ABILITY_BIND_REQUIRES_ELEMENT.send(user, ability.displayName(), ability.element().displayName());
      return;
    }
    if (slot == 0) {
      slot = user.currentSlot();
    }
    user.bindAbility(slot, ability);
    Message.ABILITY_BIND_SUCCESS.send(user, ability.displayName(), slot);
  }

  private void onBindClear(User user, int slot) {
    if (slot == 0) {
      user.bindPreset(Preset.empty());
      Message.CLEAR_ALL_SLOTS.send(user);
      return;
    }
    user.clearSlot(slot);
    Message.CLEAR_SLOT.send(user, slot);
  }

  private void onBindList(C sender, User user) {
    Collection<Element> elements = user.elements();
    Component hover;
    if (elements.isEmpty()) {
      hover = Message.NO_ELEMENTS.build();
    } else {
      JoinConfiguration sep = JoinConfiguration.commas(true);
      hover = Component.join(sep, elements.stream().map(Element::displayName).toList())
        .colorIfAbsent(ColorPalette.TEXT_COLOR);
    }
    Message.BOUND_SLOTS.send(sender, user.name().hoverEvent(HoverEvent.showText(hover)));
    user.slots().display().forEach(sender::sendMessage);
  }
}
