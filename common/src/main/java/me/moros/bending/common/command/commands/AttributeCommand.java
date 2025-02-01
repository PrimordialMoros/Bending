/*
 * Copyright 2020-2025 Moros
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
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.command.Permissions;
import me.moros.bending.common.command.parser.AbilityParser;
import me.moros.bending.common.locale.Message;
import me.moros.bending.common.util.Initializer;
import me.moros.bending.common.util.ReflectionUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.incendo.cloud.minecraft.extras.RichDescription;

public record AttributeCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    commander().register(commander().rootBuilder()
      .literal("attribute")
      .required("ability", AbilityParser.parserGlobal())
      .commandDescription(RichDescription.of(Message.ATTRIBUTE_DESC.build()))
      .permission(Permissions.ATTRIBUTE)
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
    }).exceptionally(e -> {
      commander().plugin().logger().warn(e.getMessage(), e);
      return null;
    });
  }

  private List<Component> collectAttributes(User user, AbilityDescription desc) {
    Class<Configurable> configType = ReflectionUtil.findInnerClass(desc.createAbility(), Configurable.class);
    if (configType == null) {
      return List.of();
    }
    List<Component> result = new ArrayList<>();
    var attributeValues = user.game().configProcessor().listAttributes(user, desc, configType);
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
