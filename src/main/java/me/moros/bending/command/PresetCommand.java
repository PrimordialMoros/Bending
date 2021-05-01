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

import me.moros.atlas.acf.BaseCommand;
import me.moros.atlas.acf.CommandHelp;
import me.moros.atlas.acf.annotation.CommandAlias;
import me.moros.atlas.acf.annotation.CommandCompletion;
import me.moros.atlas.acf.annotation.CommandPermission;
import me.moros.atlas.acf.annotation.Default;
import me.moros.atlas.acf.annotation.Description;
import me.moros.atlas.acf.annotation.HelpCommand;
import me.moros.atlas.acf.annotation.Subcommand;
import me.moros.bending.locale.Message;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

@CommandAlias("%presetcommand")
@CommandPermission("bending.command.preset")
public class PresetCommand extends BaseCommand {
  @Default
  @Subcommand("list|ls|l")
  @Description("List all available presets")
  public static void onPresetList(BendingPlayer player) {
    Collection<String> presets = player.presets();
    if (presets.isEmpty()) {
      Message.NO_PRESETS.send(player);
    } else {
      player.entity().sendMessage(Component.text(String.join(", ", presets), NamedTextColor.GREEN));
    }
  }

  @HelpCommand
  @CommandPermission("bending.command.help")
  public static void doHelp(CommandSender user, CommandHelp help) {
    user.sendMessage(Message.HELP_HEADER.build());
    help.showHelp();
  }

  @Subcommand("create|c")
  @Description("Create a new preset")
  public static void onPresetCreate(BendingPlayer player, String name) {
    String input = ChatUtil.sanitizeInput(name);
    Preset preset = player.createPresetFromSlots(input);
    if (preset.isEmpty()) {
      Message.EMPTY_PRESET.send(player);
      return;
    }
    player.addPreset(preset, result -> player.entity().sendMessage(result.message(input)));
  }

  @Subcommand("remove|rm|r|delete|del|d")
  @CommandCompletion("@presets")
  @Description("Remove an existing preset")
  public static void onPresetRemove(BendingPlayer player, Preset preset) {
    if (player.removePreset(preset)) {
      Message.PRESET_REMOVE_SUCCESS.send(player, preset.name());
    } else {
      Message.PRESET_REMOVE_FAIL.send(player, preset.name());
    }
  }

  @Subcommand("bind|b")
  @CommandCompletion("@presets")
  @Description("Bind an existing preset")
  public static void onPresetBind(BendingPlayer player, Preset preset) {
    int count = player.bindPreset(preset);
    if (count > 0) {
      Message.PRESET_BIND_SUCCESS.send(player, count, preset.name());
    } else {
      Message.PRESET_BIND_FAIL.send(player, preset.name());
    }
  }
}
