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

import java.util.ArrayList;
import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.AttributeValue;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.command.parser.AbilityDescriptionParser;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.common.util.ReflectionUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.description.Description;

public record AttributeCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    commander().register(commander().rootBuilder()
      .literal("attribute", "attributes")
      .required("ability", AbilityDescriptionParser.parser(true))
      .commandDescription(Description.of("View all available attribute values for a specific ability"))
      .permission(CommandPermissions.ATTRIBUTE)
      .senderType(commander().playerType())
      .handler(c -> onViewConfig(c.get(ContextKeys.BENDING_PLAYER), c.get("ability")))
    );
  }

  private void onViewConfig(User user, AbilityDescription desc) {
    Tasker.async().submit(() -> {
      List<Component> attributeInfo = collectAttributes(user, desc);
      if (attributeInfo.isEmpty()) {
        Message.ATTRIBUTE_LIST_EMPTY.send(user, desc.displayName());
      } else {
        Message.ATTRIBUTE_LIST_HEADER.send(user, desc.displayName());
        attributeInfo.forEach(user::sendMessage);
      }
    });
  }

  // TODO caching?
  private List<Component> collectAttributes(User user, AbilityDescription desc) {
    Configurable config = ReflectionUtil.findStaticField(desc.createAbility(), Configurable.class);
    if (config == null) {
      return List.of();
    }
    List<Component> result = new ArrayList<>();
    var attributeValues = user.game().configProcessor().listAttributes(user, desc, config);
    for (AttributeValue av : attributeValues) {
      Component valueComponent = Component.text(String.valueOf(av.finalValue()), ColorPalette.ACCENT);
      if (av.modified()) {
        valueComponent = valueComponent
          .decorate(TextDecoration.UNDERLINED)
          .hoverEvent(HoverEvent.showText(Component.text("Base: " + av.baseValue())));
      }
      Component text = Component.text().color(ColorPalette.TEXT_COLOR)
        .append(Component.text(av.attribute().value()).hoverEvent(HoverEvent.showText(Component.text(av.name()))))
        .append(Component.text(": "))
        .append(valueComponent)
        .build();
      result.add(text);
    }
    return result;
  }
}
