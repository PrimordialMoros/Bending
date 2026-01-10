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

import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.locale.Message;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.suggestion.BlockingSuggestionProvider;

public final class UserParser<C extends Audience> implements ArgumentParser<C, User>, BlockingSuggestionProvider.Strings<C> {
  private UserParser() {
  }

  @Override
  public ArgumentParseResult<User> parse(CommandContext<C> commandContext, CommandInput commandInput) {
    String input = commandInput.peekString();
    User result;
    if (input.equalsIgnoreCase("me")) {
      result = commandContext.getOrDefault(ContextKeys.BENDING_PLAYER, null);
    } else {
      result = Registries.BENDERS.fromString(input);
    }
    if (result == null) {
      for (User user : Registries.BENDERS) {
        if (input.equalsIgnoreCase(mapName(user))) {
          result = user;
          break;
        }
      }
    }
    if (result != null) {
      commandInput.readString();
      return ArgumentParseResult.success(result);
    }
    return ArgumentParseResult.failure(new ComponentException(Message.USER_PARSE_EXCEPTION.build(input)));
  }

  @Override
  public Iterable<String> stringSuggestions(CommandContext<C> commandContext, CommandInput commandInput) {
    Predicate<User> canSee = commandContext.optional(ContextKeys.BENDING_PLAYER)
      .map(Player.class::cast).map(u -> (Predicate<User>) u::canSee).orElse(e -> true);
    return Registries.BENDERS.stream().filter(canSee).map(this::mapName).toList();
  }

  private String mapName(User user) {
    return user.get(Identity.NAME).orElseGet(() -> PlainTextComponentSerializer.plainText().serialize(user.name()));
  }

  public static <C extends Audience> ParserDescriptor<C, User> parser() {
    return ParserDescriptor.of(new UserParser<>(), User.class);
  }
}
