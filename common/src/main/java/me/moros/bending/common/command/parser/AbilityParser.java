/*
 * Copyright 2020-2026 Moros
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

import java.util.function.Predicate;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.CommandUtil;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.locale.Message;
import net.kyori.adventure.audience.Audience;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public final class AbilityParser<C extends Audience> implements ArgumentParser<C, AbilityDescription>, BlockingSuggestionProvider.Strings<C> {
  private final boolean validBindsOnly;

  private AbilityParser(boolean validBindsOnly) {
    this.validBindsOnly = validBindsOnly;
  }

  @Override
  public ArgumentParseResult<AbilityDescription> parse(CommandContext<C> commandContext, CommandInput commandInput) {
    String input = commandInput.peekString();
    AbilityDescription check = Registries.ABILITIES.fromString(input);
    if (check != null && !check.hidden()) {
      commandInput.readString();
      return ArgumentParseResult.success(check);
    } else {
      return ArgumentParseResult.failure(new ComponentException(Message.ABILITY_PARSE_EXCEPTION.build(input)));
    }
  }

  @Override
  public Iterable<String> stringSuggestions(CommandContext<C> commandContext, CommandInput commandInput) {
    Predicate<AbilityDescription> predicate = d -> !d.hidden();
    User user = commandContext.optional(ContextKeys.BENDING_PLAYER).orElse(null);
    if (validBindsOnly) {
      predicate = predicate.and(AbilityDescription::canBind);
      if (user != null) {
        predicate = predicate.and(desc -> user.hasElements(desc.elements()));
      }
    }
    if (user != null) {
      predicate = predicate.and(user::hasPermission);
    }
    return Registries.ABILITIES.stream().filter(predicate).map(CommandUtil::mapToSuggestion).toList();
  }

  public static <C extends Audience> ParserDescriptor<C, AbilityDescription> parser() {
    return ParserDescriptor.of(new AbilityParser<>(true), AbilityDescription.class);
  }

  public static <C extends Audience> ParserDescriptor<C, AbilityDescription> parserGlobal() {
    return ParserDescriptor.of(new AbilityParser<>(false), AbilityDescription.class);
  }
}
