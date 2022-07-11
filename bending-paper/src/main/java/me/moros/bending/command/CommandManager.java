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
import java.util.UUID;
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
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import net.kyori.adventure.identity.Identity;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CommandManager extends PaperCommandManager<CommandSender> {
  public CommandManager(Bending plugin, Game game) throws Exception {
    super(plugin, CommandExecutionCoordinator.simpleCoordinator(), Function.identity(), Function.identity());
    registerExceptionHandler();
    registerAsynchronousCompletions();
    commandSuggestionProcessor(this::suggestionProvider);
    registerCommandPreProcessor(this::preprocessor);
    registerParsers();
    new BendingCommand(plugin, game, this);
  }

  private void registerExceptionHandler() {
    new MinecraftExceptionHandler<CommandSender>().withDefaultHandlers().withDecorator(Message::brand)
      .apply(this, AudienceProvider.nativeAudience());
  }

  public Builder<CommandSender> rootBuilder() {
    return commandBuilder("bending", "bend", "b", "avatar", "atla", "tla")
      .meta(CommandMeta.DESCRIPTION, "Base command for bending");
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
    context.getCommandContext().getSender().get(Identity.UUID).ifPresent(uuid -> {
      if (Registries.BENDERS.get(uuid) instanceof BendingPlayer bendingPlayer) {
        context.getCommandContext().store(ContextKeys.BENDING_PLAYER, bendingPlayer);
      }
    });
  }

  private void registerParsers() {
    parserRegistry().registerParserSupplier(TypeToken.get(AbilityDescription.class), options -> new AbilityDescriptionParser());
    parserRegistry().registerParserSupplier(TypeToken.get(ModifyPolicy.class), options -> new ModifyPolicyParser());
    parserRegistry().registerParserSupplier(TypeToken.get(Preset.class), options -> new PresetParser());
    parserRegistry().registerParserSupplier(TypeToken.get(User.class), options -> new UserParser());
  }

  public static List<String> combinedSuggestions(CommandSender sender) {
    return Stream.of(Element.NAMES, abilityCompletions(sender, false)).flatMap(Collection::stream).toList();
  }

  public static List<String> abilityCompletions(@Nullable CommandSender sender, boolean validOnly) {
    Predicate<AbilityDescription> predicate = x -> true;
    if (validOnly) {
      predicate = AbilityDescription::canBind;
    }
    Predicate<AbilityDescription> hasPermission = x -> true;
    Predicate<AbilityDescription> hasElement = x -> true;
    UUID uuid = sender == null ? null : sender.getOrDefault(Identity.UUID, null);
    if (uuid != null && Registries.BENDERS.get(uuid) instanceof BendingPlayer bendingPlayer) {
      predicate = predicate.and(bendingPlayer::hasPermission);
      if (validOnly) {
        hasElement = d -> bendingPlayer.hasElement(d.element());
      }
    }
    return Registries.ABILITIES.stream().filter(desc -> !desc.hidden()).filter(predicate)
      .filter(hasElement).filter(hasPermission).map(AbilityDescription::name).toList();
  }
}
