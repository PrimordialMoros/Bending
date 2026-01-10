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

package me.moros.bending.paper.platform;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import me.moros.bending.paper.platform.CommandSender.PlayerCommandSender;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.entity.Player;

public sealed class CommandSender implements ForwardingAudience.Single permits PlayerCommandSender {
  private final CommandSourceStack stack;
  private final Audience audience;

  private CommandSender(CommandSourceStack source, Audience audience) {
    this.stack = source;
    this.audience = audience;
  }

  public CommandSourceStack stack() {
    return this.stack;
  }

  @Override
  public Audience audience() {
    return audience;
  }

  public static final class PlayerCommandSender extends CommandSender {
    private PlayerCommandSender(CommandSourceStack stack, Player player) {
      super(stack, player);
    }
  }

  public static CommandSender from(CommandSourceStack stack) {
    if (stack.getExecutor() instanceof Player player) {
      return new PlayerCommandSender(stack, player);
    } else if (stack.getSender() instanceof Player player) {
      return new PlayerCommandSender(stack, player);
    }
    return new CommandSender(stack, stack.getSender());
  }
}
