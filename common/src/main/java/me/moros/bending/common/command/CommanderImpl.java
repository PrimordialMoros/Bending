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

package me.moros.bending.common.command;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.CommandManager;
import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.AudienceProvider;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.common.Bending;
import me.moros.bending.common.command.commands.BindCommand;
import me.moros.bending.common.command.commands.BoardCommand;
import me.moros.bending.common.command.commands.ElementCommand;
import me.moros.bending.common.command.commands.HelpCommand;
import me.moros.bending.common.command.commands.ModifierCommand;
import me.moros.bending.common.command.commands.PresetCommand;
import me.moros.bending.common.command.commands.ReloadCommand;
import me.moros.bending.common.command.commands.ToggleCommand;
import me.moros.bending.common.command.commands.VersionCommand;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;

record CommanderImpl<C extends Audience>(CommandManager<C> manager, Class<? extends C> playerType,
                                         Bending plugin) implements Commander<C> {
  @Override
  public void init() {
    registerExceptionHandler();
    manager().registerCommandPreProcessor(this::preprocessor);
    Collection<Function<Commander<C>, Initializer>> cmds = List.of(
      HelpCommand::new, VersionCommand::new, ReloadCommand::new,
      BoardCommand::new, ToggleCommand::new,
      BindCommand::new, ElementCommand::new,
      ModifierCommand::new, PresetCommand::new
    );
    cmds.forEach(cmd -> cmd.apply(this).init());
  }

  @Override
  public Builder<C> rootBuilder() {
    return manager().commandBuilder("bending", "bend", "b", "avatar", "atla", "tla")
      .meta(CommandMeta.DESCRIPTION, "Base command for bending");
  }

  @Override
  public void register(Builder<C> builder) {
    manager().command(builder);
  }

  private void registerExceptionHandler() {
    new MinecraftExceptionHandler<C>().withDefaultHandlers().withDecorator(Message::brand)
      .apply(manager(), AudienceProvider.nativeAudience());
  }

  private void preprocessor(CommandPreprocessingContext<C> context) {
    context.getCommandContext().getSender().get(Identity.UUID).ifPresent(uuid -> {
      if (Registries.BENDERS.get(uuid) instanceof BendingPlayer bendingPlayer) {
        context.getCommandContext().store(ContextKeys.BENDING_PLAYER, bendingPlayer);
      }
    });
  }
}
