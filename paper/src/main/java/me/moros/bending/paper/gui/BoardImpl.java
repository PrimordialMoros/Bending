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

package me.moros.bending.paper.gui;

import io.papermc.paper.adventure.PaperAdventure;
import me.moros.bending.api.user.User;
import me.moros.bending.common.adapter.Sidebar;
import me.moros.bending.common.locale.Message;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;

public final class BoardImpl extends Sidebar {
  public BoardImpl(User user) {
    super(MinecraftServer.getServer(), user);
    init(Message.BENDING_BOARD_TITLE.build());
  }

  @Override
  protected Component emptySlot(int slot) {
    return Message.BENDING_BOARD_EMPTY_SLOT.build(slot);
  }

  @Override
  protected net.minecraft.network.chat.Component toNative(Component component) {
    return PaperAdventure.asVanilla(component);
  }
}
