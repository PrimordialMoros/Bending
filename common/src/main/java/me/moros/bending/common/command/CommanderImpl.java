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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import me.moros.bending.api.locale.Message;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.Bending;
import me.moros.bending.common.command.commands.AttributeCommand;
import me.moros.bending.common.command.commands.BackupCommand;
import me.moros.bending.common.command.commands.BindCommand;
import me.moros.bending.common.command.commands.BoardCommand;
import me.moros.bending.common.command.commands.ElementCommand;
import me.moros.bending.common.command.commands.HelpCommand;
import me.moros.bending.common.command.commands.ModifierCommand;
import me.moros.bending.common.command.commands.PresetCommand;
import me.moros.bending.common.command.commands.ReloadCommand;
import me.moros.bending.common.command.commands.ToggleCommand;
import me.moros.bending.common.command.commands.VersionCommand;
import me.moros.bending.common.command.parser.ComponentException;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.execution.preprocessor.CommandPreprocessingContext;
import org.incendo.cloud.minecraft.extras.MinecraftExceptionHandler;
import org.incendo.cloud.minecraft.extras.RichDescription;

record CommanderImpl<C extends Audience>(CommandManager<C> manager, Class<? extends C> playerType,
                                         Bending plugin) implements Commander<C> {
  @Override
  public void init() {
    registerExceptionHandler();
    manager().registerCommandPreProcessor(this::preprocessor);
    Collection<Function<Commander<C>, Initializer>> cmds = List.of(
      HelpCommand::new, VersionCommand::new, ReloadCommand::new, BackupCommand::new,
      BoardCommand::new, ToggleCommand::new,
      BindCommand::new, ElementCommand::new,
      ModifierCommand::new, AttributeCommand::new, PresetCommand::new
    );
    cmds.forEach(cmd -> cmd.apply(this).init());
  }

  @Override
  public Command.Builder<C> rootBuilder() {
    return manager().commandBuilder("bending", RichDescription.of(Message.BASE_DESC.build()), "bend", "b");
  }

  @Override
  public void register(Command.Builder<? extends C> builder) {
    manager().command(builder);
  }

  private void registerExceptionHandler() {
    MinecraftExceptionHandler.<C>createNative().defaultHandlers().decorator(Message::brand)
      .handler(ComponentException.class, (f, ctx) -> ctx.exception().componentMessage())
      .registerTo(manager());
  }

  private void preprocessor(CommandPreprocessingContext<C> context) {
    context.commandContext().sender().get(Identity.UUID).ifPresent(uuid -> {
      User user = Registries.BENDERS.get(uuid);
      if (user instanceof Player) {
        context.commandContext().store(ContextKeys.BENDING_PLAYER, user);
      }
    });
  }
}
