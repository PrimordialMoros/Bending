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

import java.util.function.Predicate;
import java.util.stream.Stream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityDescription.Sequence;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.CommandUtil;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.locale.Message;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.minecraft.extras.ImmutableMinecraftHelp;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public record HelpCommand<C extends Audience>(Commander<C> commander, MinecraftHelp<C> help) implements Initializer {
  public HelpCommand(Commander<C> commander) {
    this(commander, createHelp(commander.manager()));
  }

  @Override
  public void init() {
    var builder = commander().rootBuilder();
    commander().register(builder
      .commandDescription(RichDescription.of(Message.BASE_DESC.build()))
      .handler(c -> onHelp(c, ""))
    );
    commander().register(builder
      .literal("help")
      .optional("query", StringParser.greedyStringParser(), helpSuggestions())
      .commandDescription(RichDescription.of(Message.HELP_DESC.build()))
      .permission(CommandPermissions.HELP)
      .handler(c -> onHelp(c, c.getOrDefault("query", "")))
    );
  }

  private BlockingSuggestionProvider.Strings<C> helpSuggestions() {
    return (c, i) -> {
      Predicate<AbilityDescription> predicate = c.optional(ContextKeys.BENDING_PLAYER)
        .map(this::permissionFilter).orElse(d -> true);
      return Stream.concat(
        Element.NAMES.stream(),
        Registries.ABILITIES.stream().filter(d -> !d.hidden()).filter(predicate).map(CommandUtil::mapToSuggestion)
      ).toList();
    };
  }

  private Predicate<AbilityDescription> permissionFilter(User user) {
    return user::hasPermission;
  }

  private void onHelp(CommandContext<C> context, String rawQuery) {
    C sender = context.sender();
    if (!rawQuery.isEmpty()) {
      int index = rawQuery.indexOf(' ');
      String query = rawQuery.substring(0, index > 0 ? index : rawQuery.length());
      if (query.length() <= 5) {
        Element element = Element.fromName(query);
        if (element != null) {
          onElementInfo(context, element);
          return;
        }
      }
      AbilityDescription result = Registries.ABILITIES.fromString(query);
      if (result != null && !result.hidden() && permissionPredicate(context).test(result)) {
        onAbilityInfo(sender, result);
        return;
      }
    }
    help.queryCommands(rawQuery, sender);
  }

  private void onAbilityInfo(C sender, AbilityDescription ability) {
    Component instructions;
    if (ability instanceof Sequence sequence) {
      instructions = sequence.instructions();
    } else {
      instructions = Message.ABILITY_INSTRUCTIONS.build(ability);
    }
    Message.ABILITY_INFO_DESCRIPTION.send(sender, ability.displayName(), Message.ABILITY_DESCRIPTION.build(ability));
    Message.ABILITY_INFO_INSTRUCTIONS.send(sender, ability.displayName(), instructions);
  }

  private void onElementInfo(CommandContext<C> context, Element element) {
    var user = context.sender();
    var display = CommandUtil.collectAll(permissionPredicate(context), element);
    if (display.isEmpty()) {
      Message.ELEMENT_ABILITIES_EMPTY.send(user, element.displayName());
    } else {
      Message.ELEMENT_ABILITIES_HEADER.send(user, element.displayName(), element.description());
      display.forEach(user::sendMessage);
    }
  }

  private Predicate<AbilityDescription> permissionPredicate(CommandContext<C> context) {
    return desc -> desc.permissions().stream().allMatch(context::hasPermission);
  }

  private static <C extends Audience> MinecraftHelp<C> createHelp(CommandManager<C> manager) {
    return ImmutableMinecraftHelp.copyOf(MinecraftHelp.createNative("/bending help", manager))
      .withMaxResultsPerPage(9)
      .withColors(MinecraftHelp.helpColors(
        ColorPalette.NEUTRAL,
        ColorPalette.TEXT_COLOR,
        ColorPalette.ACCENT,
        ColorPalette.HEADER,
        ColorPalette.NEUTRAL)
      );
  }
}
