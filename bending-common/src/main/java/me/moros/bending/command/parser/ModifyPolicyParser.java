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

package me.moros.bending.command.parser;

import java.util.List;
import java.util.Queue;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import me.moros.bending.command.BendingCommandManager;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.registry.Registries;
import net.kyori.adventure.audience.Audience;

public final class ModifyPolicyParser<T extends Audience> implements ArgumentParser<T, ModifyPolicy> {
  @Override
  public ArgumentParseResult<ModifyPolicy> parse(CommandContext<T> commandContext, Queue<String> inputQueue) {
    String input = inputQueue.peek();
    if (input == null) {
      return ArgumentParseResult.failure(new NoInputProvidedException(ModifyPolicyParser.class, commandContext));
    }
    Element element = Element.fromName(input);
    if (element != null) {
      inputQueue.remove();
      return ArgumentParseResult.success(ModifyPolicy.of(element));
    }
    AbilityDescription desc = Registries.ABILITIES.fromString(input);
    if (desc != null) {
      inputQueue.remove();
      return ArgumentParseResult.success(ModifyPolicy.of(desc));
    }
    return ArgumentParseResult.failure(new Throwable("Could not match policy " + input));
  }

  @Override
  public List<String> suggestions(final CommandContext<T> commandContext, final String input) {
    return BendingCommandManager.combinedSuggestions(commandContext.getSender());
  }

  @Override
  public boolean isContextFree() {
    return true;
  }
}