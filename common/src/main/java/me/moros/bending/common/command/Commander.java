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

package me.moros.bending.common.command;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.CommandManager;
import me.moros.bending.common.Bending;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;

public interface Commander<C> extends Initializer {
  Bending plugin();

  Class<? extends C> playerType();

  CommandManager<C> manager();

  Builder<C> rootBuilder();

  void register(Builder<C> builder);

  static <C extends Audience> Commander<C> create(CommandManager<C> manager, Class<? extends C> playerType, Bending plugin) {
    return new CommanderImpl<>(manager, playerType, plugin);
  }
}
