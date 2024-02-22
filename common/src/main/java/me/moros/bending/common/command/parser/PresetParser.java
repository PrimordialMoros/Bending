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

import java.util.List;

import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.locale.Message;
import net.kyori.adventure.audience.Audience;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public final class PresetParser<C extends Audience> implements ArgumentParser<C, Preset>, BlockingSuggestionProvider.Strings<C> {
  @Override
  public ArgumentParseResult<Preset> parse(CommandContext<C> commandContext, CommandInput commandInput) {
    String input = commandInput.peekString();
    Preset preset = commandContext.optional(ContextKeys.BENDING_PLAYER)
      .map(u -> u.presetByName(input)).orElse(null);
    if (preset != null) {
      commandInput.readString();
      return ArgumentParseResult.success(preset);
    } else {
      return ArgumentParseResult.failure(new ComponentException(Message.PRESET_PARSE_EXCEPTION.build(input)));
    }
  }

  @Override
  public Iterable<String> stringSuggestions(CommandContext<C> commandContext, CommandInput commandInput) {
    return commandContext.optional(ContextKeys.BENDING_PLAYER).map(User::presets)
      .map(p -> p.stream().map(Preset::name).toList()).orElseGet(List::of);
  }

  public static <C extends Audience> ParserDescriptor<C, Preset> parser() {
    return ParserDescriptor.of(new PresetParser<>(), Preset.class);
  }
}


