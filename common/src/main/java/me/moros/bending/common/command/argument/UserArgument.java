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
import java.util.function.Predicate;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.ContextKeys;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class UserArgument<C extends Audience> extends CommandArgument<C, User> {
  private UserArgument(boolean required, String name, String defaultValue,
                       @Nullable BiFunction<CommandContext<C>, String, List<String>> suggestionsProvider,
                       ArgumentDescription defaultDescription) {
    super(required, name, new Parser<>(), defaultValue, User.class, suggestionsProvider, defaultDescription);
  }

  public static <C extends Audience> Builder<C> builder(String name) {
    return new UserArgument.Builder<>(name);
  }

  public static <C extends Audience> UserArgument<C> of(String name) {
    return UserArgument.<C>builder(name).build();
  }

  public static <C extends Audience> UserArgument<C> optional(String name) {
    return UserArgument.<C>builder(name).asOptional().build();
  }

  public static <C extends Audience> UserArgument<C> optional(String name, String defaultValue) {
    return UserArgument.<C>builder(name).asOptionalWithDefault(defaultValue).build();
  }

  public static final class Builder<C extends Audience> extends TypedBuilder<C, User, Builder<C>> {
    private Builder(String name) {
      super(User.class, name);
    }

    @Override
    public UserArgument<C> build() {
      return new UserArgument<>(
        this.isRequired(),
        this.getName(),
        this.getDefaultValue(),
        this.getSuggestionsProvider(),
        this.getDefaultDescription()
      );
    }
  }

  public static final class Parser<C extends Audience> implements ArgumentParser<C, User> {
    @Override
    public ArgumentParseResult<User> parse(CommandContext<C> commandContext, Queue<String> inputQueue) {
      String input = inputQueue.peek();
      if (input == null) {
        return ArgumentParseResult.failure(new NoInputProvidedException(Parser.class, commandContext));
      }
      inputQueue.remove();
      if (input.equalsIgnoreCase("me")) {
        User user = commandContext.getOrDefault(ContextKeys.BENDING_PLAYER, null);
        if (user != null) {
          return ArgumentParseResult.success(user);
        }
      }
      User result = Registries.BENDERS.fromString(input);
      if (result == null) {
        for (User user : Registries.BENDERS) {
          if (input.equalsIgnoreCase(user.getOrDefault(Identity.NAME, ""))) {
            result = user;
            break;
          }
        }
      }
      if (result == null) {
        return ArgumentParseResult.failure(new Throwable("Could not find user " + input));
      }
      return ArgumentParseResult.success(result);
    }

    @Override
    public List<String> suggestions(CommandContext<C> commandContext, String input) {
      Predicate<User> canSee = commandContext.getOptional(ContextKeys.BENDING_PLAYER)
        .map(u -> (Predicate<User>) u::canSee).orElse(e -> true);
      return Registries.BENDERS.stream().filter(canSee).map(this::mapName).toList();
    }

    private String mapName(User user) {
      return user.get(Identity.NAME).orElseGet(() -> PlainTextComponentSerializer.plainText().serialize(user.name()));
    }
  }
}


