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

import cloud.commandframework.Command.Builder;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.minecraft.extras.MinecraftHelp.HelpColors;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityDescription.Sequence;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.CommandUtil;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;

public record HelpCommand<C extends Audience>(Commander<C> commander, MinecraftHelp<C> help) implements Initializer {
  public HelpCommand(Commander<C> commander) {
    this(commander, createHelp(commander.manager()));
  }

  @Override
  public void init() {
    Builder<C> builder = commander().rootBuilder();
    var helpArg = StringArgument.<C>builder("query").greedy()
      .withSuggestionsProvider((c, s) -> CommandUtil.combinedSuggestions(c.getSender())).asOptional().build();
    commander().register(builder.handler(c -> onHelp(c, "")));
    commander().register(builder.literal("help", "h")
      .meta(CommandMeta.DESCRIPTION, "View info about an element, ability or command")
      .permission(CommandPermissions.HELP)
      .argument(helpArg)
      .handler(c -> onHelp(c, c.getOrDefault("query", "")))
    );
  }

  private void onHelp(CommandContext<C> context, String rawQuery) {
    C sender = context.getSender();
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
    var translationManager = commander().plugin().translationManager();
    Component description = translationManager.translate(ability.descriptionKey());
    Component instructions = translationManager.translate(ability.instructionsKey());
    if (instructions == null && ability instanceof Sequence sequence) {
      instructions = sequence.instructions();
    }
    if (description == null && instructions == null) {
      Message.ABILITY_INFO_EMPTY.send(sender, ability.displayName());
    } else {
      if (description != null) {
        Message.ABILITY_INFO_DESCRIPTION.send(sender, ability.displayName(), description);
      }
      if (instructions != null) {
        Message.ABILITY_INFO_INSTRUCTIONS.send(sender, ability.displayName(), instructions);
      }
    }
  }

  private void onElementInfo(CommandContext<C> context, Element element) {
    var user = context.getSender();
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
    var help = MinecraftHelp.createNative("/bending help", manager);
    help.setMaxResultsPerPage(9);
    help.setHelpColors(HelpColors.of(
      ColorPalette.NEUTRAL,
      ColorPalette.TEXT_COLOR,
      ColorPalette.ACCENT,
      ColorPalette.HEADER,
      ColorPalette.NEUTRAL)
    );
    return help;
  }
}
