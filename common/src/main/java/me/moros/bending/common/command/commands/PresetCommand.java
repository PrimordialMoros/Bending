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

package me.moros.bending.common.command.commands;

import java.util.Collection;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.TextUtil;
import me.moros.bending.common.command.CommandPermissions;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.command.argument.PresetArgument;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

public record PresetCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    Builder<C> builder = commander().rootBuilder().literal("preset", "presets", "p")
      .permission(CommandPermissions.PRESET);
    commander().register(builder.literal("list", "ls")
      .meta(CommandMeta.DESCRIPTION, "List all available presets")
      .senderType(commander().playerType())
      .handler(c -> onPresetList(c.get(ContextKeys.BENDING_PLAYER)))
    );
    commander().register(builder.literal("create", "c")
      .meta(CommandMeta.DESCRIPTION, "Create a new preset")
      .senderType(commander().playerType())
      .argument(StringArgument.single("name"))
      .handler(c -> onPresetCreate(c.get(ContextKeys.BENDING_PLAYER), c.get("name")))
    );
    commander().register(builder.literal("remove", "rm")
      .meta(CommandMeta.DESCRIPTION, "Remove an existing preset")
      .senderType(commander().playerType())
      .argument(PresetArgument.of("preset"))
      .handler(c -> onPresetRemove(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
    );
    commander().register(builder.literal("bind", "b")
      .meta(CommandMeta.DESCRIPTION, "Bind an existing preset")
      .senderType(commander().playerType())
      .argument(PresetArgument.of("preset"))
      .handler(c -> onPresetBind(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
    );
  }

  private void onPresetList(User user) {
    Collection<Preset> presets = user.presets();
    if (presets.isEmpty()) {
      Message.NO_PRESETS.send(user);
    } else {
      Message.PRESET_LIST_HEADER.send(user);
      JoinConfiguration sep = JoinConfiguration.commas(true);
      user.sendMessage(Component.join(sep, presets.stream().map(Preset::meta).toList()).colorIfAbsent(ColorPalette.TEXT_COLOR));
    }
  }

  private void onPresetCreate(User user, String name) {
    String input = TextUtil.sanitizeInput(name);
    if (input.isEmpty()) {
      Message.INVALID_PRESET_NAME.send(user);
      return;
    }
    Preset preset = user.slots();
    if (preset.isEmpty()) {
      Message.EMPTY_PRESET.send(user);
      return;
    }
    user.addPreset(preset.withName(input)).send(user, input);
  }

  private void onPresetRemove(User user, Preset preset) {
    if (user.removePreset(preset)) {
      Message.PRESET_REMOVE_SUCCESS.send(user, preset.name());
    } else {
      Message.PRESET_REMOVE_FAIL.send(user, preset.name());
    }
  }

  private void onPresetBind(User user, Preset preset) {
    if (user.bindPreset(preset)) {
      Message.PRESET_BIND_SUCCESS.send(user, preset.name());
    } else {
      Message.PRESET_BIND_FAIL.send(user, preset.name());
    }
  }
}
