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

package me.moros.bending.locale;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.command.ConsoleCommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.Component.translatable;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * @see TranslationManager
 */
public interface Message {
  Component PREFIX = text("[", DARK_GRAY)
    .append(text("Bending", DARK_AQUA))
    .append(text("] ", DARK_GRAY));

  Args0 HELP_HEADER = () -> brand(translatable("bending.command.help.header", DARK_AQUA));

  Args0 NO_PRESETS = () -> translatable("bending.command.preset.list-not-found", YELLOW);
  Args0 EMPTY_PRESET = () -> translatable("bending.command.preset.create-empty", YELLOW);

  Args1<String> PRESET_SUCCESS = preset -> translatable("bending.command.preset.create-success", GREEN)
    .args(text(preset));
  Args1<String> PRESET_EXISTS = preset -> translatable("bending.command.preset.create-exists", YELLOW)
    .args(text(preset));
  Args1<String> PRESET_CANCELLED = preset -> translatable("bending.command.preset.create-cancelled", YELLOW)
    .args(text(preset));
  Args1<String> PRESET_FAIL = preset -> translatable("bending.command.preset.create-fail", RED)
    .args(text(preset));

  Args1<String> PRESET_REMOVE_SUCCESS = preset -> translatable("bending.command.preset.remove-success", GREEN)
    .args(text(preset));
  Args1<String> PRESET_REMOVE_FAIL = preset -> translatable("bending.command.preset.remove-fail", RED)
    .args(text(preset));

  Args2<Integer, String> PRESET_BIND_SUCCESS = (amount, preset) -> translatable("bending.command.preset.bind-success", GREEN)
    .args(text(amount), text(preset));
  Args1<String> PRESET_BIND_FAIL = preset -> translatable("bending.command.preset.bind-fail", YELLOW);

  Args1<String> MODIFIER_ADD = name -> translatable("bending.command.modifier.add", GREEN)
    .args(text(name));
  Args1<String> MODIFIER_CLEAR = name -> translatable("bending.command.modifier.clear", GREEN)
    .args(text(name));

  Args0 TOGGLE_ON = () -> translatable("bending.command.toggle.on", GREEN);
  Args0 TOGGLE_OFF = () -> translatable("bending.command.toggle.off", RED);

  Args0 CONFIG_RELOAD = () -> translatable("bending.command.config-reload", GREEN);

  Args1<Component> ELEMENT_CHOOSE_NO_PERMISSION = element -> translatable("bending.command.element.choose-no-permission", RED)
    .args(element);
  Args1<Component> ELEMENT_CHOOSE_SUCCESS = element -> translatable("bending.command.element.choose-success", GREEN)
    .args(element);
  Args1<Component> ELEMENT_CHOOSE_FAIL = element -> translatable("bending.command.element.choose-fail", YELLOW)
    .args(element);

  Args1<Component> ELEMENT_ADD_NO_PERMISSION = element -> translatable("bending.command.element.add-no-permission", RED)
    .args(element);
  Args1<Component> ELEMENT_ADD_SUCCESS = element -> translatable("bending.command.element.add-success", GREEN)
    .args(element);
  Args1<Component> ELEMENT_ADD_FAIL = element -> translatable("bending.command.element.add-fail", YELLOW)
    .args(element);

  Args1<Component> ELEMENT_REMOVE_SUCCESS = element -> translatable("bending.command.element.remove-success", GRAY)
    .args(element);
  Args1<Component> ELEMENT_REMOVE_FAIL = element -> translatable("bending.command.element.remove-fail", YELLOW)
    .args(element);

  Args0 BOARD_DISABLED = () -> translatable("bending.command.board.disabled", RED);
  Args0 BOARD_TOGGLED_ON = () -> translatable("bending.command.board.on", GREEN);
  Args0 BOARD_TOGGLED_OFF = () -> translatable("bending.command.board.off", YELLOW);

  Args1<Component> ELEMENT_ABILITIES_HEADER = element -> translatable("bending.command.display.abilities-header", DARK_AQUA)
    .args(element);

  Args1<Component> ELEMENT_ABILITIES_EMPTY = element -> translatable("bending.command.display.abilities-not-found", YELLOW)
    .args(element);

  Args0 ABILITIES = () -> translatable("bending.command.display.abilities", DARK_GRAY);
  Args0 SEQUENCES = () -> translatable("bending.command.display.sequences", DARK_GRAY);
  Args0 PASSIVES = () -> translatable("bending.command.display.passives", DARK_GRAY);

  Args2<Component, Component> ABILITY_BIND_REQUIRES_ELEMENT = (ability, element) -> translatable("bending.command.bind.require-element", YELLOW)
    .args(ability, element);

  Args2<Component, Integer> ABILITY_BIND_SUCCESS = (ability, slot) -> translatable("bending.command.bind.success", GREEN)
    .args(ability, text(slot));

  Args1<Component> ABILITY_BIND_FAIL = ability -> translatable("bending.command.bind.fail", YELLOW)
    .args(ability);

  Args1<String> BOUND_SLOTS = name -> translatable("bending.command.display.header", DARK_AQUA)
    .args(text(name));

  Args0 CLEAR_ALL_SLOTS = () -> translatable("bending.command.clear.all", GREEN);
  Args1<Integer> CLEAR_SLOT = slot -> translatable("bending.command.clear.specific", GREEN)
    .args(text(slot));

  Args1<Component> ABILITY_INFO_EMPTY = ability -> translatable("bending.command.info.empty", YELLOW)
    .args(ability);

  Args2<Component, Component> ABILITY_INFO_DESCRIPTION = (ability, description) -> translatable("bending.command.info.description", TextColor.color(221, 221, 221))
    .args(ability, description);

  Args2<Component, Component> ABILITY_INFO_INSTRUCTIONS = (ability, instructions) -> translatable("bending.command.info.instructions", TextColor.color(221, 221, 221))
    .args(ability, instructions);

  Args2<String, String> VERSION_COMMAND_HOVER = (author, link) -> translatable("bending.command.version.hover", DARK_AQUA)
    .args(text(author, GREEN), text(link, GREEN));

  Args0 BENDING_BOARD_TITLE = () -> translatable("bending.board.title", Style.style(TextDecoration.BOLD));

  // Scoreboard items not translatable yet?
  Args2<String, String> BENDING_BOARD_EMPTY_SLOT = (prefix, slot) -> text(prefix).append(text("-- Slot " + slot + " --", DARK_GRAY));

  static @NonNull Component brand(@NonNull ComponentLike message) {
    return text().append(PREFIX).append(message).build();
  }

  interface Args0 {
    @NonNull Component build();

    default void send(@NonNull Audience audience) {
      if (audience instanceof ConsoleCommandSender) {
        audience.sendMessage(GlobalTranslator.render(build(), TranslationManager.DEFAULT_LOCALE));
        return;
      }
      audience.sendMessage(build());
    }
  }

  interface Args1<A0> {
    @NonNull Component build(@NonNull A0 arg0);

    default void send(@NonNull Audience audience, @NonNull A0 arg0) {
      if (audience instanceof ConsoleCommandSender) {
        audience.sendMessage(GlobalTranslator.render(build(arg0), TranslationManager.DEFAULT_LOCALE));
        return;
      }
      audience.sendMessage(build(arg0));
    }
  }

  interface Args2<A0, A1> {
    @NonNull Component build(@NonNull A0 arg0, @NonNull A1 arg1);

    default void send(@NonNull Audience audience, @NonNull A0 arg0, @NonNull A1 arg1) {
      if (audience instanceof ConsoleCommandSender) {
        audience.sendMessage(GlobalTranslator.render(build(arg0, arg1), TranslationManager.DEFAULT_LOCALE));
        return;
      }
      audience.sendMessage(build(arg0, arg1));
    }
  }
}
