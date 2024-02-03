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

package me.moros.bending.common.command.parser;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.command.CommandUtil;
import net.kyori.adventure.audience.Audience;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public final class AbilityDescriptionParser<C extends Audience> implements ArgumentParser<C, AbilityDescription>, BlockingSuggestionProvider.Strings<C> {
  private final boolean sequenceSuggestions;

  public AbilityDescriptionParser(boolean sequenceSuggestions) {
    this.sequenceSuggestions = sequenceSuggestions;
  }

  @Override
  public ArgumentParseResult<AbilityDescription> parse(CommandContext<C> commandContext, CommandInput commandInput) {
    String input = commandInput.readString();
    AbilityDescription check = Registries.ABILITIES.fromString(input);
    if (check != null && !check.hidden()) {
      return ArgumentParseResult.success(check);
    } else {
      return ArgumentParseResult.failure(new Throwable("Could not find ability " + input));
    }
  }

  @Override
  public Iterable<String> stringSuggestions(CommandContext<C> commandContext, CommandInput commandInput) {
    return CommandUtil.abilityCompletions(commandContext.sender(), true, sequenceSuggestions);
  }

  public static <C extends Audience> ParserDescriptor<C, AbilityDescription> parser(boolean sequenceSuggestions) {
    return ParserDescriptor.of(new AbilityDescriptionParser<>(sequenceSuggestions), AbilityDescription.class);
  }
}


