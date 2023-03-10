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
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.command.CommandUtil;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class AbilityDescriptionArgument<C extends Audience> extends CommandArgument<C, AbilityDescription> {
  private AbilityDescriptionArgument(boolean required, String name, String defaultValue,
                                     @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
                                     ArgumentDescription defaultDescription) {
    super(required, name, new Parser<>(), defaultValue, AbilityDescription.class, suggestionsProvider, defaultDescription);
  }

  public static <C extends Audience> Builder<C> builder(String name) {
    return new AbilityDescriptionArgument.Builder<>(name);
  }

  public static <C extends Audience> AbilityDescriptionArgument<C> of(String name) {
    return AbilityDescriptionArgument.<C>builder(name).build();
  }

  public static <C extends Audience> AbilityDescriptionArgument<C> optional(String name) {
    return AbilityDescriptionArgument.<C>builder(name).asOptional().build();
  }

  public static <C extends Audience> AbilityDescriptionArgument<C> optional(String name, String defaultValue) {
    return AbilityDescriptionArgument.<C>builder(name).asOptionalWithDefault(defaultValue).build();
  }

  public static final class Builder<C extends Audience> extends TypedBuilder<C, AbilityDescription, Builder<C>> {
    private Builder(String name) {
      super(AbilityDescription.class, name);
    }

    @Override
    public AbilityDescriptionArgument<C> build() {
      return new AbilityDescriptionArgument<>(
        this.isRequired(),
        this.getName(),
        this.getDefaultValue(),
        this.getSuggestionsProvider(),
        this.getDefaultDescription()
      );
    }
  }

  public static final class Parser<C extends Audience> implements ArgumentParser<C, AbilityDescription> {
    @Override
    public ArgumentParseResult<AbilityDescription> parse(CommandContext<C> commandContext, Queue<String> inputQueue) {
      String input = inputQueue.peek();
      if (input != null) {
        inputQueue.remove();
        AbilityDescription check = Registries.ABILITIES.fromString(input);
        if (check != null && !check.hidden()) {
          return ArgumentParseResult.success(check);
        } else {
          return ArgumentParseResult.failure(new Throwable("Could not find ability " + input));
        }
      }
      return ArgumentParseResult.failure(new NoInputProvidedException(Parser.class, commandContext));
    }

    @Override
    public List<String> suggestions(CommandContext<C> commandContext, String input) {
      return CommandUtil.abilityCompletions(commandContext.getSender(), true);
    }

    @Override
    public boolean isContextFree() {
      return true;
    }
  }
}


