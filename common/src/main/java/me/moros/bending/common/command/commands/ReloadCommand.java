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

import me.moros.bending.api.locale.Message;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import org.incendo.cloud.minecraft.extras.RichDescription;

public record ReloadCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    commander().register(commander().rootBuilder()
      .literal("reload")
      .commandDescription(RichDescription.of(Message.RELOAD_DESC.build()))
      .permission(CommandPermissions.RELOAD)
      .handler(c -> onReload(c.sender()))
    );
  }

  private void onReload(C sender) {
    commander().plugin().reload();
    Message.RELOAD.send(sender);
  }
}
