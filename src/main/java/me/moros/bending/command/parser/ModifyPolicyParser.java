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

package me.moros.bending.command.parser;

import java.util.List;
import java.util.Optional;
import java.util.Queue;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import me.moros.bending.command.CommandManager;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.registry.Registries;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ModifyPolicyParser implements ArgumentParser<CommandSender, ModifyPolicy> {
  @Override
  public @NonNull ArgumentParseResult<ModifyPolicy> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull Queue<@NonNull String> inputQueue) {
    String input = inputQueue.peek();
    Optional<Element> element = Element.fromName(input);
    if (element.isPresent()) {
      inputQueue.remove();
      return ArgumentParseResult.success(ModifyPolicy.of(element.get()));
    }
    AbilityDescription desc = Registries.ABILITIES.ability(input);
    if (desc != null) {
      inputQueue.remove();
      return ArgumentParseResult.success(ModifyPolicy.of(desc));
    }
    return ArgumentParseResult.failure(new Throwable("Could not match policy " + input));
  }

  @Override
  public @NonNull List<@NonNull String> suggestions(final @NonNull CommandContext<CommandSender> commandContext, final @NonNull String input) {
    return CommandManager.combinedSuggestions(commandContext.getSender());
  }
}
