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

package me.moros.bending.sponge.platform;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.entity.living.player.server.ServerPlayer;

public class CommandSender implements ForwardingAudience.Single {
  private final CommandCause cause;

  private CommandSender(CommandCause cause) {
    this.cause = cause;
  }

  public CommandCause cause() {
    return this.cause;
  }

  @Override
  public @NonNull Audience audience() {
    return cause.audience();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof CommandSender other) {
      return cause.root().equals(other.cause.root());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return cause.root().hashCode();
  }

  public static final class PlayerCommandSender extends CommandSender {
    private PlayerCommandSender(CommandCause cause) {
      super(cause);
    }
  }

  public static CommandSender from(CommandCause cause) {
    if (cause.root() instanceof ServerPlayer) {
      return new PlayerCommandSender(cause);
    }
    return new CommandSender(cause);
  }
}
