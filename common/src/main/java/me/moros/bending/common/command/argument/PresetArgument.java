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

package me.moros.bending.common.command.argument;

import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.common.command.ContextKeys;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class PresetArgument<C extends Audience> extends CommandArgument<C, Preset> {
  private PresetArgument(boolean required, String name, String defaultValue,
                         @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
                         ArgumentDescription defaultDescription) {
    super(required, name, new Parser<>(), defaultValue, Preset.class, suggestionsProvider, defaultDescription);
  }

  public static <C extends Audience> Builder<C> builder(String name) {
    return new PresetArgument.Builder<>(name);
  }

  public static <C extends Audience> PresetArgument<C> of(String name) {
    return PresetArgument.<C>builder(name).build();
  }

  public static <C extends Audience> PresetArgument<C> optional(String name) {
    return PresetArgument.<C>builder(name).asOptional().build();
  }

  public static <C extends Audience> PresetArgument<C> optional(String name, String defaultValue) {
    return PresetArgument.<C>builder(name).asOptionalWithDefault(defaultValue).build();
  }

  public static final class Builder<C extends Audience> extends TypedBuilder<C, Preset, Builder<C>> {
    private Builder(String name) {
      super(Preset.class, name);
    }

    @Override
    public PresetArgument<C> build() {
      return new PresetArgument<>(
        this.isRequired(),
        this.getName(),
        this.getDefaultValue(),
        this.getSuggestionsProvider(),
        this.getDefaultDescription()
      );
    }
  }

  public static final class Parser<C extends Audience> implements ArgumentParser<C, Preset> {
    @Override
    public ArgumentParseResult<Preset> parse(CommandContext<C> commandContext, Queue<String> inputQueue) {
      String input = inputQueue.peek();
      if (input == null) {
        return ArgumentParseResult.failure(new NoInputProvidedException(Parser.class, commandContext));
      }
      Preset preset = commandContext.getOptional(ContextKeys.BENDING_PLAYER)
        .map(u -> u.presetByName(input)).orElse(null);
      if (preset != null) {
        inputQueue.remove();
        return ArgumentParseResult.success(preset);
      } else {
        return ArgumentParseResult.failure(new Throwable("Could not find preset " + input));
      }
    }

    @Override
    public List<String> suggestions(CommandContext<C> commandContext, String input) {
      return commandContext.getOptional(ContextKeys.BENDING_PLAYER).map(BendingPlayer::presets)
        .map(p -> p.stream().map(Preset::name).toList()).orElseGet(List::of);
    }
  }
}


