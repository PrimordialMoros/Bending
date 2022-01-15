/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.command;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import cloud.commandframework.paper.PaperCommandManager;
import io.leangen.geantyref.TypeToken;
import me.moros.bending.Bending;
import me.moros.bending.command.parser.AbilityDescriptionParser;
import me.moros.bending.command.parser.ModifyPolicyParser;
import me.moros.bending.command.parser.PresetParser;
import me.moros.bending.command.parser.UserParser;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CommandManager extends PaperCommandManager<CommandSender> {
  public CommandManager(@NonNull Bending plugin) throws Exception {
    super(plugin, CommandExecutionCoordinator.simpleCoordinator(), Function.identity(), Function.identity());
    registerExceptionHandler();
    registerAsynchronousCompletions();
    setCommandSuggestionProcessor(this::suggestionProvider);
    registerCommandPreProcessor(this::preprocessor);
    registerParsers();
    new BendingCommand(this);
  }

  private void registerExceptionHandler() {
    new MinecraftExceptionHandler<CommandSender>().withDefaultHandlers().withDecorator(Message::brand)
      .apply(this, AudienceProvider.nativeAudience());
  }

  public @NonNull Builder<@NonNull CommandSender> rootBuilder() {
    return commandBuilder("bending", "bend", "b", "avatar", "atla", "tla")
      .meta(CommandMeta.DESCRIPTION, "Base command for bending");
  }

  public static @NonNull List<@NonNull String> combinedSuggestions(@NonNull CommandSender sender) {
    return Stream.of(CommandManager.abilityCompletions(sender, false), Element.NAMES)
      .flatMap(Collection::stream).toList();
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

  private void preprocessor(CommandPreprocessingContext<CommandSender> context) {
    if (context.getCommandContext().getSender() instanceof Player player) {
      context.getCommandContext().store(ContextKeys.BENDING_PLAYER, Registries.BENDERS.user(player));
    }
  }

  private void registerParsers() {
    getParserRegistry().registerParserSupplier(TypeToken.get(AbilityDescription.class), options -> new AbilityDescriptionParser());
    getParserRegistry().registerParserSupplier(TypeToken.get(ModifyPolicy.class), options -> new ModifyPolicyParser());
    getParserRegistry().registerParserSupplier(TypeToken.get(Preset.class), options -> new PresetParser());
    getParserRegistry().registerParserSupplier(TypeToken.get(User.class), options -> new UserParser());
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
