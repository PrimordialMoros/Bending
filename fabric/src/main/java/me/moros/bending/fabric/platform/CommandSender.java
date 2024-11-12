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

package me.moros.bending.fabric.platform;

import me.moros.bending.fabric.platform.CommandSender.PlayerCommandSender;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public sealed class CommandSender implements ForwardingAudience.Single permits PlayerCommandSender {
  private final CommandSourceStack stack;

  private CommandSender(CommandSourceStack source) {
    this.stack = source;
  }

  public CommandSourceStack stack() {
    return this.stack;
  }

  @Override
  public Audience audience() {
    return stack;
  }

  public static final class PlayerCommandSender extends CommandSender {
    private PlayerCommandSender(CommandSourceStack stack) {
      super(stack);
    }
  }

  public static CommandSender from(CommandSourceStack stack) {
    if (stack.getPlayer() instanceof ServerPlayer) {
      return new PlayerCommandSender(stack);
    }
    return new CommandSender(stack);
  }
}
