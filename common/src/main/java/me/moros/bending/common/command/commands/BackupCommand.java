/*
 * Copyright 2020-2025 Moros
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

import me.moros.bending.api.GameProvider;
import me.moros.bending.api.storage.BendingStorage;
import me.moros.bending.common.backup.Operation;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.Permissions;
import me.moros.bending.common.locale.Message;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.parser.standard.StringParser;

public record BackupCommand<C extends Audience>(Commander<C> commander, AtomicBoolean running) implements Initializer {
  public BackupCommand(Commander<C> commander) {
    this(commander, new AtomicBoolean());
  }

  @Override
  public void init() {
    var builder = commander().rootBuilder();
    commander().register(builder
      .literal("export")
      .optional("file", StringParser.quotedStringParser(), DefaultValue.constant(""))
      .commandDescription(RichDescription.of(Message.EXPORT_DESC.build()))
      .permission(Permissions.EXPORT)
      .handler(c -> onExport(c.sender(), c.get("file")))
    );
    commander().register(builder
      .literal("import")
      .required("file", StringParser.quotedStringParser())
      .commandDescription(RichDescription.of(Message.IMPORT_DESC.build()))
      .permission(Permissions.IMPORT)
      .handler(c -> onImport(c.sender(), c.get("file")))
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
