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
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.incendo.cloud.description.Description;

public record VersionCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    commander().register(commander().rootBuilder()
      .literal("version", "v")
      .commandDescription(Description.of("View version info about the bending plugin"))
      .permission(CommandPermissions.VERSION)
      .handler(c -> onVersion(c.sender()))
    );
  }


  private void onVersion(C sender) {
    String link = "https://github.com/PrimordialMoros/Bending";
    Component version = Component.text("Bending " + commander().plugin().version(), ColorPalette.HEADER)
      .hoverEvent(HoverEvent.showText(Message.VERSION_COMMAND_HOVER.build(commander().plugin().author(), link)))
      .clickEvent(ClickEvent.openUrl(link));
    sender.sendMessage(version);
  }
}
