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

package me.moros.bending.common.command.commands;

import me.moros.bending.api.user.User;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.command.Permissions;
import me.moros.bending.common.locale.Message;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import org.incendo.cloud.minecraft.extras.RichDescription;

public record ToggleCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    commander().register(commander().rootBuilder()
      .literal("toggle")
      .commandDescription(RichDescription.of(Message.TOGGLE_DESC.build()))
      .permission(Permissions.TOGGLE)
      .senderType(commander().playerType())
      .handler(c -> onToggle(c.get(ContextKeys.BENDING_PLAYER)))
    );
  }

  private void onToggle(User user) {
    if (user.toggleBending()) {
      Message.TOGGLE_ON.send(user);
    } else {
      Message.TOGGLE_OFF.send(user);
    }
  }
}
