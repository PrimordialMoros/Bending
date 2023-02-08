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

package me.moros.bending.common.command.parser;

import java.util.List;
import java.util.Queue;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.common.command.ContextKeys;
import net.kyori.adventure.audience.Audience;

public final class PresetParser<T extends Audience> implements ArgumentParser<T, Preset> {
  @Override
  public ArgumentParseResult<Preset> parse(CommandContext<T> commandContext, Queue<String> inputQueue) {
    String input = inputQueue.peek();
    if (input != null) {
      inputQueue.remove();
      Preset preset = commandContext.get(ContextKeys.BENDING_PLAYER).presetByName(input);
      if (preset != null) {
        return ArgumentParseResult.success(preset);
      } else {
        return ArgumentParseResult.failure(new Throwable("Could not find preset " + input));
      }
    }
    return ArgumentParseResult.failure(new NoInputProvidedException(PresetParser.class, commandContext));
  }

  @Override
  public List<String> suggestions(final CommandContext<T> commandContext, final String input) {
    BendingPlayer player = commandContext.getOrDefault(ContextKeys.BENDING_PLAYER, null);
    return player == null ? List.of() : player.presets().stream().map(Preset::name).toList();
  }
}