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

package me.moros.bending.fabric.platform;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.moros.bending.fabric.mixin.accessor.CommandSourceStackAccess;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointer;
import net.kyori.adventure.util.TriState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CommandSender implements ForwardingAudience.Single {
  private final CommandSourceStack stack;

  private CommandSender(CommandSourceStack source) {
    this.stack = source;
  }

  public CommandSourceStack stack() {
    return this.stack;
  }

  @Override
  public @NonNull Audience audience() {
    return stack;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof CommandSender other) {
      return ((CommandSourceStackAccess) this.stack).source().equals(((CommandSourceStackAccess) other.stack).source());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ((CommandSourceStackAccess) this.stack).source().hashCode();
  }

  public static final class PlayerCommandSender extends CommandSender implements PermissionChecker {
    private final ServerPlayer player;

    private PlayerCommandSender(CommandSourceStack stack, ServerPlayer player) {
      super(stack);
      this.player = player;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> @Nullable T getOrDefault(Pointer<T> pointer, @Nullable T defaultValue) {
      if (pointer == PermissionChecker.POINTER) {
        return (T) this;
      }
      return super.getOrDefault(pointer, defaultValue);
    }

    @Override
    public @NonNull TriState value(String permission) {
      return TriState.byBoolean(Permissions.check(player, permission, player.getLevel().getServer().getOperatorUserPermissionLevel()));
    }
  }

  public static CommandSender from(CommandSourceStack stack) {
    if (((CommandSourceStackAccess) stack).source() instanceof ServerPlayer player) {
      return new PlayerCommandSender(stack, player);
    }
    return new CommandSender(stack);
  }
}
