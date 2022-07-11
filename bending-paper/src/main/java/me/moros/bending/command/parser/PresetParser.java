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
import java.util.Queue;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import me.moros.bending.command.ContextKeys;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import org.bukkit.command.CommandSender;

public final class PresetParser implements ArgumentParser<CommandSender, Preset> {
  @Override
  public ArgumentParseResult<Preset> parse(CommandContext<CommandSender> commandContext, Queue<String> inputQueue) {
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
  public List<String> suggestions(final CommandContext<CommandSender> commandContext, final String input) {
    BendingPlayer player = commandContext.getOrDefault(ContextKeys.BENDING_PLAYER, null);
    return player == null ? List.of() : player.presets().stream().map(Preset::name).toList();
  }
}
