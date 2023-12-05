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
import java.util.Optional;
import java.util.Queue;
import java.util.function.BiFunction;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.config.attribute.ModifyPolicy;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.common.command.CommandUtil;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ModifyPolicyArgument<C extends Audience> extends CommandArgument<C, ModifyPolicy> {
  private ModifyPolicyArgument(boolean required, String name, String defaultValue,
                               @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
                               ArgumentDescription defaultDescription) {
    super(required, name, new Parser<>(), defaultValue, ModifyPolicy.class, suggestionsProvider, defaultDescription);
  }

  public static <C extends Audience> Builder<C> builder(String name) {
    return new ModifyPolicyArgument.Builder<>(name);
  }

  public static <C extends Audience> ModifyPolicyArgument<C> of(String name) {
    return ModifyPolicyArgument.<C>builder(name).build();
  }

  public static <C extends Audience> ModifyPolicyArgument<C> optional(String name) {
    return ModifyPolicyArgument.<C>builder(name).asOptional().build();
  }

  public static <C extends Audience> ModifyPolicyArgument<C> optional(String name, String defaultValue) {
    return ModifyPolicyArgument.<C>builder(name).asOptionalWithDefault(defaultValue).build();
  }

  public static final class Builder<C extends Audience> extends TypedBuilder<C, ModifyPolicy, Builder<C>> {
    private Builder(String name) {
      super(ModifyPolicy.class, name);
    }

    @Override
    public ModifyPolicyArgument<C> build() {
      return new ModifyPolicyArgument<>(
        this.isRequired(),
        this.getName(),
        this.getDefaultValue(),
        this.getSuggestionsProvider(),
        this.getDefaultDescription()
      );
    }
  }

  public static final class Parser<C extends Audience> implements ArgumentParser<C, ModifyPolicy> {
    @Override
    public ArgumentParseResult<ModifyPolicy> parse(CommandContext<C> commandContext, Queue<String> inputQueue) {
      String input = inputQueue.peek();
      if (input == null) {
        return ArgumentParseResult.failure(new NoInputProvidedException(Parser.class, commandContext));
      }
      ModifyPolicy result = Optional.ofNullable(Element.fromName(input)).map(ModifyPolicy::of)
        .orElseGet(() -> {
          AbilityDescription desc = Registries.ABILITIES.fromString(input);
          return desc == null ? null : ModifyPolicy.of(desc);
        });
      if (result != null) {
        inputQueue.remove();
        return ArgumentParseResult.success(result);
      }
      return ArgumentParseResult.failure(new Throwable("Could not match policy " + input));
    }

    @Override
    public List<String> suggestions(CommandContext<C> commandContext, String input) {
      return CommandUtil.combinedSuggestions(commandContext.getSender());
    }
  }
}


