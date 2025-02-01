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

import java.util.Collection;

import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.ability.preset.PresetRegisterResult;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.TextUtil;
import me.moros.bending.common.command.CommandUtil;
import me.moros.bending.common.command.Commander;
import me.moros.bending.common.command.ContextKeys;
import me.moros.bending.common.command.Permissions;
import me.moros.bending.common.command.parser.PresetParser;
import me.moros.bending.common.locale.Message;
import me.moros.bending.common.util.Initializer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.incendo.cloud.minecraft.extras.RichDescription;
import org.incendo.cloud.parser.standard.StringParser;

public record PresetCommand<C extends Audience>(Commander<C> commander) implements Initializer {
  @Override
  public void init() {
    var builder = commander().rootBuilder().literal("preset")
      .commandDescription(RichDescription.of(Message.PRESET_DESC.build()))
      .permission(Permissions.PRESET);
    commander().register(builder
      .literal("list")
      .commandDescription(RichDescription.of(Message.PRESET_LIST_DESC.build()))
      .senderType(commander().playerType())
      .handler(c -> onPresetList(c.get(ContextKeys.BENDING_PLAYER)))
    );
    commander().register(builder
      .literal("create")
      .required("name", StringParser.stringParser())
      .commandDescription(RichDescription.of(Message.PRESET_CREATE_DESC.build()))
      .senderType(commander().playerType())
      .handler(c -> onPresetCreate(c.get(ContextKeys.BENDING_PLAYER), c.get("name")))
    );
    commander().register(builder
      .literal("remove")
      .required("preset", PresetParser.parser())
      .commandDescription(RichDescription.of(Message.PRESET_REMOVE_DESC.build()))
      .senderType(commander().playerType())
      .handler(c -> onPresetRemove(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
    );
    commander().register(builder
      .literal("bind")
      .required("preset", PresetParser.parser())
      .commandDescription(RichDescription.of(Message.PRESET_BIND_DESC.build()))
      .senderType(commander().playerType())
      .handler(c -> onPresetBind(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
    );
  }

  private void onPresetList(User user) {
    Collection<Preset> presets = user.presets();
    if (presets.isEmpty()) {
      Message.NO_PRESETS.send(user);
    } else {
      Message.PRESET_LIST_HEADER.send(user, user.presetSize());
      JoinConfiguration sep = JoinConfiguration.commas(true);
      Component presetList = Component.join(sep, presets.stream().map(CommandUtil::presetDescription).toList());
      user.sendMessage(presetList.colorIfAbsent(ColorPalette.TEXT_COLOR));
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
    var result = user.addPreset(preset.withName(input));
    user.sendMessage(mapPresetResult(result, input));
  }

  private Component mapPresetResult(PresetRegisterResult result, String name) {
    var msg = switch (result) {
      case SUCCESS -> Message.PRESET_SUCCESS;
      case EXISTS -> Message.PRESET_EXISTS;
      case CANCELLED -> Message.PRESET_CANCELLED;
      case FAIL -> Message.PRESET_FAIL;
    };
    return msg.build(name);
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
