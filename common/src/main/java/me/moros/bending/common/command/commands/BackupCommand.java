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

package me.moros.bending.common.command.commands;

import java.util.concurrent.atomic.AtomicBoolean;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.arguments.standard.StringArgument.StringMode;
import cloud.commandframework.meta.CommandMeta;
import me.moros.bending.api.GameProvider;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.common.backup.Operation;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;

public record BackupCommand<C extends Audience>(Commander<C> commander, AtomicBoolean running) implements Initializer {
  public BackupCommand(Commander<C> commander) {
    this(commander, new AtomicBoolean());
  }

  @Override
  public void init() {
    Builder<C> builder = commander().rootBuilder();
    commander().register(builder.literal("export")
      .meta(CommandMeta.DESCRIPTION, "Export all saved data")
      .permission(CommandPermissions.EXPORT)
      .argument(StringArgument.optional("file", StringMode.QUOTED))
      .handler(c -> onExport(c.getSender(), c.getOrDefault("file", "")))
    );
    commander().register(builder.literal("import")
      .meta(CommandMeta.DESCRIPTION, "Import data from file")
      .permission(CommandPermissions.IMPORT)
      .argument(StringArgument.of("file", StringMode.QUOTED))
      .handler(c -> onImport(c.getSender(), c.get("file")))
    );
  }

  private void onExport(C sender, String fileName) {
    BendingStorage storage = GameProvider.get().storage();
    if (running.compareAndSet(false, true)) {
      Operation.ofExport(commander().plugin(), storage, sender, fileName).execute(running);
    } else {
      Message.BACKUP_ALREADY_RUNNING.send(sender);
    }
  }

  private void onImport(C sender, String fileName) {
    BendingStorage storage = GameProvider.get().storage();
    if (running.compareAndSet(false, true)) {
      Operation.ofImport(commander().plugin(), storage, sender, fileName).execute(running);
    } else {
      Message.BACKUP_ALREADY_RUNNING.send(sender);
    }
  }
}
