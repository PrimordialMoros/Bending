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

import java.util.Optional;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.config.attribute.ModifyPolicy;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.command.CommandUtil;
import net.kyori.adventure.audience.Audience;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public final class ModifyPolicyParser<C extends Audience> implements ArgumentParser<C, ModifyPolicy>, BlockingSuggestionProvider.Strings<C> {
  @Override
  public ArgumentParseResult<ModifyPolicy> parse(CommandContext<C> commandContext, CommandInput commandInput) {
    String input = commandInput.readString();
    ModifyPolicy result = Optional.ofNullable(Element.fromName(input)).map(ModifyPolicy::of)
      .orElseGet(() -> {
        AbilityDescription desc = Registries.ABILITIES.fromString(input);
        return desc == null ? null : ModifyPolicy.of(desc);
      });
    if (result != null) {
      return ArgumentParseResult.success(result);
    }
    return ArgumentParseResult.failure(new Throwable("Could not match policy " + input));
  }

  @Override
  public Iterable<String> stringSuggestions(CommandContext<C> commandContext, CommandInput commandInput) {
    return CommandUtil.combinedSuggestions(commandContext.sender());
  }

  public static <C extends Audience> ParserDescriptor<C, ModifyPolicy> parser() {
    return ParserDescriptor.of(new ModifyPolicyParser<>(), ModifyPolicy.class);
  }
}


