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

package me.moros.bending.common.locale;

import me.moros.bending.api.ability.AbilityDescription;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import static me.moros.bending.api.util.ColorPalette.*;
import static net.kyori.adventure.text.Component.*;

/**
 * Provides formatted messages.
 */
public interface Message {
  Component PREFIX = text("[", ACCENT)
    .append(text("Bending", TEXT_COLOR))
    .append(text("] ", ACCENT));

  Args1<String> ABILITY_PARSE_EXCEPTION = input -> translatable("bending.command.parse.ability")
    .arguments(text(input));
  Args1<String> PRESET_PARSE_EXCEPTION = input -> translatable("bending.command.parse.preset")
    .arguments(text(input));
  Args1<String> USER_PARSE_EXCEPTION = input -> translatable("bending.command.parse.user")
    .arguments(text(input));

  Args0 BASE_DESC = () -> translatable("bending.command.description");

  Args0 PRESET_DESC = () -> translatable("bending.command.preset.description");
  Args0 PRESET_LIST_DESC = () -> translatable("bending.command.preset.list.description");
  Args0 INVALID_PRESET_NAME = () -> translatable("bending.command.preset.invalid-name", FAIL);
  Args0 NO_PRESETS = () -> translatable("bending.command.preset.list-not-found", WARN);
  Args1<Integer> PRESET_LIST_HEADER = count -> translatable("bending.command.preset.list.header", HEADER)
    .arguments(text(count, NEUTRAL));
  Args0 EMPTY_PRESET = () -> translatable("bending.command.preset.create-empty", WARN);
  Args0 HOVER_PRESET = () -> translatable("bending.command.preset.hover", NEUTRAL);

  Args0 PRESET_CREATE_DESC = () -> translatable("bending.command.preset.create.description");
  Args1<String> PRESET_SUCCESS = preset -> translatable("bending.command.preset.register-success", SUCCESS)
    .arguments(text(preset));
  Args1<String> PRESET_EXISTS = preset -> translatable("bending.command.preset.register-exists", WARN)
    .arguments(text(preset));
  Args1<String> PRESET_CANCELLED = preset -> translatable("bending.command.preset.register-cancelled", WARN)
    .arguments(text(preset));
  Args1<String> PRESET_FAIL = preset -> translatable("bending.command.preset.register-fail", FAIL)
    .arguments(text(preset));
  Args1<Integer> PRESET_LIMIT = limit -> translatable("bending.command.preset.register-limit", WARN)
    .arguments(text(limit, NEUTRAL));

  Args0 PRESET_REMOVE_DESC = () -> translatable("bending.command.preset.remove.description");
  Args1<String> PRESET_REMOVE_SUCCESS = preset -> translatable("bending.command.preset.remove-success", SUCCESS)
    .arguments(text(preset));
  Args1<String> PRESET_REMOVE_FAIL = preset -> translatable("bending.command.preset.remove-fail", FAIL)
    .arguments(text(preset));

  Args0 PRESET_BIND_DESC = () -> translatable("bending.command.preset.bind.description");
  Args1<String> PRESET_BIND_SUCCESS = preset -> translatable("bending.command.preset.bind-success", SUCCESS)
    .arguments(text(preset));
  Args1<String> PRESET_BIND_FAIL = preset -> translatable("bending.command.preset.bind-fail", WARN)
    .arguments(text(preset));

  Args0 MODIFIER_DESC = () -> translatable("bending.command.modifier.description");
  Args0 MODIFIER_ADD_DESC = () -> translatable("bending.command.modifier.add.description");
  Args1<Component> MODIFIER_ADD = name -> translatable("bending.command.modifier.add", SUCCESS)
    .arguments(name);
  Args0 MODIFIER_CLEAR_DESC = () -> translatable("bending.command.modifier.clear.description");
  Args1<Component> MODIFIER_CLEAR = name -> translatable("bending.command.modifier.clear", SUCCESS)
    .arguments(name);

  Args0 ATTRIBUTE_DESC = () -> translatable("bending.command.modifier.attribute.description");
  Args1<Component> ATTRIBUTE_LIST_HEADER = name -> translatable("bending.command.attribute.list.header", HEADER)
    .arguments(name);
  Args1<Component> ATTRIBUTE_LIST_EMPTY = name -> translatable("bending.command.attribute.list.empty", WARN)
    .arguments(name);

  Args0 TOGGLE_DESC = () -> translatable("bending.command.toggle.description");
  Args0 TOGGLE_ON = () -> translatable("bending.command.toggle.on", SUCCESS);
  Args0 TOGGLE_OFF = () -> translatable("bending.command.toggle.off", FAIL);

  Args0 RELOAD_DESC = () -> translatable("bending.command.reload.description");
  Args0 RELOAD = () -> translatable("bending.command.reload", SUCCESS);

  Args1<Component> ELEMENT_TOAST_NOTIFICATION = element -> translatable("bending.command.element.toast-notification", TEXT_COLOR)
    .arguments(element);

  Args0 ELEMENT_CHOOSE_DESC = () -> translatable("bending.command.element.choose.description");
  Args1<Component> ELEMENT_CHOOSE_NO_PERMISSION = element -> translatable("bending.command.element.choose-no-permission", FAIL)
    .arguments(element);
  Args1<Component> ELEMENT_CHOOSE_SUCCESS = element -> translatable("bending.command.element.choose-success", SUCCESS)
    .arguments(element);
  Args1<Component> ELEMENT_CHOOSE_FAIL = element -> translatable("bending.command.element.choose-fail", WARN)
    .arguments(element);

  Args0 ELEMENT_ADD_DESC = () -> translatable("bending.command.element.add.description");
  Args1<Component> ELEMENT_ADD_SUCCESS = element -> translatable("bending.command.element.add-success", SUCCESS)
    .arguments(element);
  Args1<Component> ELEMENT_ADD_FAIL = element -> translatable("bending.command.element.add-fail", WARN)
    .arguments(element);

  Args0 ELEMENT_REMOVE_DESC = () -> translatable("bending.command.element.remove.description");
  Args1<Component> ELEMENT_REMOVE_SUCCESS = element -> translatable("bending.command.element.remove-success", SUCCESS)
    .arguments(element);
  Args1<Component> ELEMENT_REMOVE_FAIL = element -> translatable("bending.command.element.remove-fail", WARN)
    .arguments(element);

  Args0 BOARD_DESC = () -> translatable("bending.command.board.description");
  Args0 BOARD_DISABLED = () -> translatable("bending.command.board.disabled", FAIL);
  Args0 BOARD_TOGGLED_ON = () -> translatable("bending.command.board.on", SUCCESS);
  Args0 BOARD_TOGGLED_OFF = () -> translatable("bending.command.board.off", WARN);

  Args0 HELP_DESC = () -> translatable("bending.command.help.description");
  Args2<Component, Component> ELEMENT_ABILITIES_HEADER = (element, desc) -> translatable("bending.command.display.abilities-header", HEADER)
    .arguments(element.hoverEvent(HoverEvent.showText(desc)));

  Args1<Component> ELEMENT_ABILITIES_EMPTY = element -> translatable("bending.command.display.abilities-not-found", WARN)
    .arguments(element);

  Args0 ABILITIES = () -> translatable("bending.command.display.abilities", TEXT_COLOR);
  Args0 SEQUENCES = () -> translatable("bending.command.display.sequences", TEXT_COLOR);
  Args0 PASSIVES = () -> translatable("bending.command.display.passives", TEXT_COLOR);

  Args0 ABILITY_HOVER = () -> translatable("bending.command.display.ability-hover", NEUTRAL);

  Args0 BIND_DESC = () -> translatable("bending.command.bind.description");
  Args2<Component, Component> ABILITY_BIND_REQUIRES_ELEMENT = (ability, element) -> translatable("bending.command.bind.require-element", WARN)
    .arguments(ability, element);

  Args2<Component, Integer> ABILITY_BIND_SUCCESS = (ability, slot) -> translatable("bending.command.bind.success", SUCCESS)
    .arguments(ability, text(slot));

  Args1<Component> ABILITY_BIND_FAIL = ability -> translatable("bending.command.bind.fail", WARN)
    .arguments(ability);

  Args1<Component> ABILITY_BIND_NO_PERMISSION = ability -> translatable("bending.command.bind.no-permission", FAIL)
    .arguments(ability);

  Args0 DISPLAY_DESC = () -> translatable("bending.command.display.description");
  Args1<Component> BOUND_SLOTS = name -> translatable("bending.command.binds.header", HEADER)
    .arguments(name);

  Args0 NO_ELEMENTS = () -> translatable("bending.command.binds.no-elements", NEUTRAL);

  Args0 CLEAR_DESC = () -> translatable("bending.command.clear.description");
  Args0 CLEAR_ALL_SLOTS = () -> translatable("bending.command.clear.all", SUCCESS);
  Args1<Integer> CLEAR_SLOT = slot -> translatable("bending.command.clear.specific", SUCCESS)
    .arguments(text(slot));

  Args2<Component, Component> ABILITY_INFO_DESCRIPTION = (ability, description) -> translatable("bending.command.info.description", TEXT_COLOR)
    .arguments(ability, description);

  Args2<Component, Component> ABILITY_INFO_INSTRUCTIONS = (ability, instructions) -> translatable("bending.command.info.instructions", TEXT_COLOR)
    .arguments(ability, instructions);

  Args1<AbilityDescription> ABILITY_DESCRIPTION = desc -> translatable(desc.translationKey() + ".description");
  Args1<AbilityDescription> ABILITY_INSTRUCTIONS = desc -> translatable(desc.translationKey() + ".instructions");
  Args3<Component, Component, AbilityDescription> ABILITY_DEATH_MESSAGE = (target, killer, desc) -> translatable(desc.translationKey() + ".death",
    "bending.ability.generic.death").arguments(target, killer, desc.displayName());

  Args0 VERSION_DESC = () -> translatable("bending.command.version.description");
  Args2<String, String> VERSION_COMMAND_HOVER = (author, link) -> translatable("bending.command.version.hover", NEUTRAL)
    .arguments(text(author, HEADER), text("GNU AGPLv3", TextColor.fromHexString("#007EC6")), text(link, LINK_COLOR))
    .append(newline()).append(newline())
    .append(translatable("bending.command.version.hover.open-link", NEUTRAL));

  Args0 EXPORT_DESC = () -> translatable("bending.command.export.description");
  Args0 BACKUP_ALREADY_RUNNING = () -> brand(translatable("bending.command.backup.running", FAIL));
  Args1<Integer> EXPORT_PROGRESS = percent -> brand(translatable("bending.command.export.progress", NEUTRAL)
    .arguments(text(percent, ACCENT)));
  Args2<String, Double> EXPORT_SUCCESS = (path, seconds) -> brand(translatable("bending.command.export.success", SUCCESS)
    .arguments(text(path, ACCENT), text(seconds)));
  Args0 IMPORT_DESC = () -> translatable("bending.command.import.description");
  Args1<Integer> IMPORT_PROGRESS = percent -> brand(translatable("bending.command.import.progress", NEUTRAL)
    .arguments(text(percent, ACCENT)));
  Args1<Double> IMPORT_SUCCESS = seconds -> brand(translatable("bending.command.import.success", SUCCESS)
    .arguments(text(seconds)));

  Args0 BENDING_BOARD_TITLE = () -> translatable("bending.board.title", Style.style(TEXT_COLOR, TextDecoration.BOLD));

  Args1<Integer> BENDING_BOARD_EMPTY_SLOT = slot -> translatable("bending.board.empty-slot", METAL)
    .arguments(text(slot));

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

  @FunctionalInterface
  interface Args3<A0, A1, A2> {
    Component build(A0 arg0, A1 arg1, A2 arg2);

    default void send(Audience audience, A0 arg0, A1 arg1, A2 arg2) {
      audience.sendMessage(build(arg0, arg1, arg2));
    }
  }
}
