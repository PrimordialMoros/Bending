/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.locale;

import java.util.Locale;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static me.moros.bending.util.ColorPalette.*;
import static net.kyori.adventure.text.Component.*;

/**
 * Provides formatted messages.
 */
public interface Message {
  Locale DEFAULT_LOCALE = Locale.ENGLISH;

  Component PREFIX = text("[", ACCENT)
    .append(text("Bending", TEXT_COLOR))
    .append(text("] ", ACCENT));

  Args0 INVALID_PRESET_NAME = () -> translatable("bending.command.preset.invalid-name", FAIL);
  Args0 NO_PRESETS = () -> translatable("bending.command.preset.list-not-found", WARN);
  Args0 PRESET_LIST_HEADER = () -> translatable("bending.command.preset.list.header", HEADER);
  Args0 EMPTY_PRESET = () -> translatable("bending.command.preset.create-empty", WARN);
  Args0 HOVER_PRESET = () -> translatable("bending.command.preset.hover", NEUTRAL);

  Args1<String> PRESET_SUCCESS = preset -> translatable("bending.command.preset.create-success", SUCCESS)
    .args(text(preset));
  Args1<String> PRESET_EXISTS = preset -> translatable("bending.command.preset.create-exists", WARN)
    .args(text(preset));
  Args1<String> PRESET_CANCELLED = preset -> translatable("bending.command.preset.create-cancelled", WARN)
    .args(text(preset));
  Args1<String> PRESET_FAIL = preset -> translatable("bending.command.preset.create-fail", FAIL)
    .args(text(preset));

  Args1<String> PRESET_REMOVE_SUCCESS = preset -> translatable("bending.command.preset.remove-success", SUCCESS)
    .args(text(preset));
  Args1<String> PRESET_REMOVE_FAIL = preset -> translatable("bending.command.preset.remove-fail", FAIL)
    .args(text(preset));

  Args1<String> PRESET_BIND_SUCCESS = preset -> translatable("bending.command.preset.bind-success", SUCCESS)
    .args(text(preset));
  Args1<String> PRESET_BIND_FAIL = preset -> translatable("bending.command.preset.bind-fail", WARN)
    .args(text(preset));

  Args1<String> MODIFIER_ADD = name -> translatable("bending.command.modifier.add", SUCCESS)
    .args(text(name));
  Args1<String> MODIFIER_CLEAR = name -> translatable("bending.command.modifier.clear", SUCCESS)
    .args(text(name));

  Args0 TOGGLE_ON = () -> translatable("bending.command.toggle.on", SUCCESS);
  Args0 TOGGLE_OFF = () -> translatable("bending.command.toggle.off", FAIL);

  Args0 RELOAD = () -> translatable("bending.command.reload", SUCCESS);

  Args1<Component> ELEMENT_TOAST_NOTIFICATION = element -> translatable("bending.command.element.toast-notification", TEXT_COLOR)
    .args(element);

  Args1<Component> ELEMENT_CHOOSE_NO_PERMISSION = element -> translatable("bending.command.element.choose-no-permission", FAIL)
    .args(element);
  Args1<Component> ELEMENT_CHOOSE_SUCCESS = element -> translatable("bending.command.element.choose-success", SUCCESS)
    .args(element);
  Args1<Component> ELEMENT_CHOOSE_FAIL = element -> translatable("bending.command.element.choose-fail", WARN)
    .args(element);

  Args1<Component> ELEMENT_ADD_NO_PERMISSION = element -> translatable("bending.command.element.add-no-permission", FAIL)
    .args(element);
  Args1<Component> ELEMENT_ADD_SUCCESS = element -> translatable("bending.command.element.add-success", SUCCESS)
    .args(element);
  Args1<Component> ELEMENT_ADD_FAIL = element -> translatable("bending.command.element.add-fail", WARN)
    .args(element);

  Args1<Component> ELEMENT_REMOVE_SUCCESS = element -> translatable("bending.command.element.remove-success", SUCCESS)
    .args(element);
  Args1<Component> ELEMENT_REMOVE_FAIL = element -> translatable("bending.command.element.remove-fail", WARN)
    .args(element);

  Args0 BOARD_DISABLED = () -> translatable("bending.command.board.disabled", FAIL);
  Args0 BOARD_TOGGLED_ON = () -> translatable("bending.command.board.on", SUCCESS);
  Args0 BOARD_TOGGLED_OFF = () -> translatable("bending.command.board.off", WARN);

  Args2<Component, Component> ELEMENT_ABILITIES_HEADER = (element, desc) -> translatable("bending.command.display.abilities-header", HEADER)
    .args(element.hoverEvent(HoverEvent.showText(desc)));

  Args1<Component> ELEMENT_ABILITIES_EMPTY = element -> translatable("bending.command.display.abilities-not-found", WARN)
    .args(element);

  Args0 ABILITIES = () -> translatable("bending.command.display.abilities", TEXT_COLOR);
  Args0 SEQUENCES = () -> translatable("bending.command.display.sequences", TEXT_COLOR);
  Args0 PASSIVES = () -> translatable("bending.command.display.passives", TEXT_COLOR);

  Args0 ABILITY_HOVER = () -> translatable("bending.command.display.ability-hover", NEUTRAL);

  Args2<Component, Component> ABILITY_BIND_REQUIRES_ELEMENT = (ability, element) -> translatable("bending.command.bind.require-element", WARN)
    .args(ability, element);

  Args2<Component, Integer> ABILITY_BIND_SUCCESS = (ability, slot) -> translatable("bending.command.bind.success", SUCCESS)
    .args(ability, text(slot));

  Args1<Component> ABILITY_BIND_FAIL = ability -> translatable("bending.command.bind.fail", WARN)
    .args(ability);

  Args1<Component> ABILITY_BIND_NO_PERMISSION = ability -> translatable("bending.command.bind.no-permission", FAIL)
    .args(ability);

  Args2<String, Component> BOUND_SLOTS = (name, elements) -> translatable("bending.command.binds.header", HEADER)
    .args(text(name).hoverEvent(HoverEvent.showText(elements)));

  Args0 NO_ELEMENTS = () -> translatable("bending.command.binds.no-elements", NEUTRAL);

  Args0 CLEAR_ALL_SLOTS = () -> translatable("bending.command.clear.all", SUCCESS);
  Args1<Integer> CLEAR_SLOT = slot -> translatable("bending.command.clear.specific", SUCCESS)
    .args(text(slot));

  Args1<Component> ABILITY_INFO_EMPTY = ability -> translatable("bending.command.info.empty", WARN)
    .args(ability);

  Args2<Component, Component> ABILITY_INFO_DESCRIPTION = (ability, description) -> translatable("bending.command.info.description", TEXT_COLOR)
    .args(ability, description);

  Args2<Component, Component> ABILITY_INFO_INSTRUCTIONS = (ability, instructions) -> translatable("bending.command.info.instructions", TEXT_COLOR)
    .args(ability, instructions);

  Args2<String, String> VERSION_COMMAND_HOVER = (author, link) -> translatable("bending.command.version.hover", NEUTRAL)
    .args(text(author, HEADER), text("GNU AGPLv3", TextColor.fromHexString("#007EC6")), text(link, LINK_COLOR))
    .append(newline()).append(newline())
    .append(translatable("bending.command.version.hover.open-link", NEUTRAL));

  Args0 BENDING_BOARD_TITLE = () -> translatable("bending.board.title", Style.style(TEXT_COLOR, TextDecoration.BOLD));

  Args1<String> BENDING_BOARD_EMPTY_SLOT = slot -> translatable("bending.board.empty-slot", METAL)
    .args(text(slot));

  Args0 GUI_NO_PERMISSION = () -> translatable("bending.gui.no-permission", FAIL);

  Args0 ELEMENTS_GUI_TITLE = () -> translatable("bending.gui.elements.title", Style.style(NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

  Args0 ELEMENTS_GUI_HELP_TITLE = () -> translatable("bending.gui.elements.help-title", TEXT_COLOR);
  Args0 ELEMENTS_GUI_CHOOSE = () -> translatable("bending.gui.elements.choose", ACCENT);
  Args0 ELEMENTS_GUI_DISPLAY = () -> translatable("bending.gui.elements.display", NEUTRAL);
  Args0 ELEMENTS_GUI_ADD = () -> translatable("bending.gui.elements.add", SUCCESS);
  Args0 ELEMENTS_GUI_REMOVE = () -> translatable("bending.gui.elements.remove", FAIL);

  static Component brand(ComponentLike message) {
    return text().append(PREFIX).append(message).build();
  }

  @FunctionalInterface
  interface Args0 {
    Component build();

    default void send(Audience audience) {
      audience.sendMessage(build());
    }
  }

  @FunctionalInterface
  interface Args1<A0> {
    Component build(A0 arg0);

    default void send(Audience audience, A0 arg0) {
      audience.sendMessage(build(arg0));
    }
  }

  @FunctionalInterface
  interface Args2<A0, A1> {
    Component build(A0 arg0, A1 arg1);

    default void send(Audience audience, A0 arg0, A1 arg1) {
      audience.sendMessage(build(arg0, arg1));
    }
  }
}
