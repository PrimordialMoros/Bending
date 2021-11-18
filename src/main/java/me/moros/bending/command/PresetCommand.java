/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.command;

import java.util.Collection;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import me.moros.bending.locale.Message;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class PresetCommand {
  PresetCommand(PaperCommandManager<CommandSender> manager) {
    Builder<CommandSender> builder = manager.commandBuilder("preset", "presets", "pr", "p")
      .meta(CommandMeta.DESCRIPTION, "Base command for bending presets")
      .permission("bending.command.preset");
    manager.command(
      builder.literal("list", "ls", "l")
        .meta(CommandMeta.DESCRIPTION, "List all available presets")
        .senderType(Player.class)
        .handler(c -> onList(c.get(ContextKeys.BENDING_PLAYER)))
    ).command(
      builder.literal("create", "c")
        .meta(CommandMeta.DESCRIPTION, "Create a new preset")
        .senderType(Player.class)
        .argument(StringArgument.single("name"))
        .handler(c -> onCreate(c.get(ContextKeys.BENDING_PLAYER), c.get("name")))
    ).command(
      builder.literal("remove", "rm", "r", "delete", "del", "d")
        .meta(CommandMeta.DESCRIPTION, "Remove an existing preset")
        .senderType(Player.class)
        .argument(manager.argumentBuilder(Preset.class, "preset"))
        .handler(c -> onRemove(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
    ).command(
      builder.literal("bind", "b")
        .meta(CommandMeta.DESCRIPTION, "Bind an existing preset")
        .senderType(Player.class)
        .argument(manager.argumentBuilder(Preset.class, "preset"))
        .handler(c -> onBind(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
    );
  }

  private static void onList(BendingPlayer player) {
    Collection<Preset> presets = player.presets();
    if (presets.isEmpty()) {
      Message.NO_PRESETS.send(player);
    } else {
      JoinConfiguration sep = JoinConfiguration.separator(Component.text(", ", NamedTextColor.WHITE));
      player.sendMessage(Component.join(sep, presets.stream().map(Preset::meta).toList()));
    }
  }

  private static void onCreate(BendingPlayer player, String name) {
    String input = ChatUtil.sanitizeInput(name);
    if (input.isEmpty()) {
      Message.INVALID_PRESET_NAME.send(player);
      return;
    }
    Preset preset = player.createPresetFromSlots(input);
    if (preset.isEmpty()) {
      Message.EMPTY_PRESET.send(player);
      return;
    }
    player.addPreset(preset).thenAccept(result -> result.message().send(player, input));
  }

  private static void onRemove(BendingPlayer player, Preset preset) {
    if (player.removePreset(preset)) {
      Message.PRESET_REMOVE_SUCCESS.send(player, preset.name());
    } else {
      Message.PRESET_REMOVE_FAIL.send(player, preset.name());
    }
  }

  private static void onBind(BendingPlayer player, Preset preset) {
    if (player.bindPreset(preset)) {
      Message.PRESET_BIND_SUCCESS.send(player, preset.name());
    } else {
      Message.PRESET_BIND_FAIL.send(player, preset.name());
    }
  }
}
