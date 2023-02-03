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
import java.util.UUID;
import java.util.function.Predicate;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.api.user.User;
import me.moros.bending.common.command.ContextKeys;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class UserParser<T extends Audience> implements ArgumentParser<T, User> {
  @Override
  public ArgumentParseResult<User> parse(CommandContext<T> commandContext, Queue<String> inputQueue) {
    String input = inputQueue.peek();
    if (input == null) {
      return ArgumentParseResult.failure(new NoInputProvidedException(UserParser.class, commandContext));
    }
    inputQueue.remove();
    if (input.equalsIgnoreCase("me")) {
      User user = commandContext.getOrDefault(ContextKeys.BENDING_PLAYER, null);
      if (user != null) {
        return ArgumentParseResult.success(user);
      }
    }
    User user = Registries.BENDERS.fromString(input);
    if (user == null) {
      UUID playerUuid = commandContext.getSender().getOrDefault(Identity.UUID, null);
      if (playerUuid != null) {
        user = Registries.BENDERS.get(playerUuid);
      }
    }
    if (user == null) {
      return ArgumentParseResult.failure(new Throwable("Could not find user " + input));
    }
    return ArgumentParseResult.success(user);
  }

  @Override
  public List<String> suggestions(final CommandContext<T> commandContext, final String input) {
    Predicate<User> canSee;
    if (commandContext.getSender() instanceof BendingPlayer bp) {
      canSee = e -> bp.entity().canSee(e.entity());
    } else {
      canSee = e -> true;
    }
    return Registries.BENDERS.stream().filter(canSee).map(u -> rawName(u.name())).toList();
  }

  private static String rawName(Component name) {
    return PlainTextComponentSerializer.plainText().serialize(name);
  }
}
