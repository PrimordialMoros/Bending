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

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;

import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.parser.ParserRegistry;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import io.leangen.geantyref.TypeToken;
import me.moros.bending.Bending;
import me.moros.bending.command.parser.AbilityDescriptionParser;
import me.moros.bending.command.parser.AttributeModifierParser;
import me.moros.bending.command.parser.ModifyPolicyParser;
import me.moros.bending.command.parser.PresetParser;
import me.moros.bending.command.parser.UserParser;
import me.moros.bending.locale.Message;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Commands {
  private final PaperCommandManager<CommandSender> manager;
  private final MinecraftHelp<CommandSender> minecraftHelp;

  public Commands(@NonNull Bending plugin) throws Exception {
    Function<CommandTree<CommandSender>, CommandExecutionCoordinator<CommandSender>> executionCoordinatorFunction =
      CommandExecutionCoordinator.simpleCoordinator();
    manager = new PaperCommandManager<>(plugin, executionCoordinatorFunction, Function.identity(), Function.identity());
    minecraftHelp = new MinecraftHelp<>("/bending help", c -> c, manager);
    manager.registerAsynchronousCompletions();
    new MinecraftExceptionHandler<CommandSender>()
      .withInvalidSyntaxHandler()
      .withInvalidSenderHandler()
      .withNoPermissionHandler()
      .withArgumentParsingHandler()
      .withCommandExecutionHandler()
      .withDecorator(Message::brand)
      .apply(manager, c -> c);

    manager.setCommandSuggestionProcessor(this::suggestionProvider);

    constructCommands();
  }

  private List<String> suggestionProvider(CommandPreprocessingContext<CommandSender> context, List<String> strings) {
    String input;
    if (context.getInputQueue().isEmpty()) {
      input = "";
    } else {
      input = context.getInputQueue().peek().toLowerCase(Locale.ROOT);
    }
    List<String> suggestions = new LinkedList<>();
    for (String suggestion : strings) {
      if (suggestion.toLowerCase(Locale.ROOT).startsWith(input)) {
        suggestions.add(suggestion);
      }
    }
    return suggestions;
  }

  private void constructCommands() {
    manager.registerCommandPreProcessor(c -> {
      if (c.getCommandContext().getSender() instanceof Player player) {
        c.getCommandContext().store(ContextKeys.BENDING_PLAYER, Registries.BENDERS.user(player));
      }
    });

    ParserRegistry<CommandSender> parserRegistry = manager.getParserRegistry();
    parserRegistry.registerParserSupplier(TypeToken.get(AbilityDescription.class), options -> new AbilityDescriptionParser());
    parserRegistry.registerParserSupplier(TypeToken.get(AttributeModifier.class), options -> new AttributeModifierParser());
    parserRegistry.registerParserSupplier(TypeToken.get(ModifyPolicy.class), options -> new ModifyPolicyParser());
    parserRegistry.registerParserSupplier(TypeToken.get(Preset.class), options -> new PresetParser());
    parserRegistry.registerParserSupplier(TypeToken.get(User.class), options -> new UserParser());

    new BendingCommand(manager, minecraftHelp);
    new ElementCommand(manager);
    new PresetCommand(manager);
    new ModifyCommand(manager);
  }

  public static @NonNull List<@NonNull String> abilityCompletions(@Nullable CommandSender sender, boolean validOnly) {
    Predicate<AbilityDescription> predicate = x -> true;
    if (validOnly) {
      predicate = AbilityDescription::canBind;
    }
    Predicate<AbilityDescription> hasPermission = x -> true;
    Predicate<AbilityDescription> hasElement = x -> true;
    if (sender instanceof Player player) {
      BendingPlayer bendingPlayer = Registries.BENDERS.user(player);
      hasPermission = bendingPlayer::hasPermission;
      if (validOnly) {
        hasElement = d -> bendingPlayer.hasElement(d.element());
      }
    }
    return Registries.ABILITIES.stream().filter(desc -> !desc.hidden()).filter(predicate)
      .filter(hasElement).filter(hasPermission).map(AbilityDescription::name).toList();
  }
}
