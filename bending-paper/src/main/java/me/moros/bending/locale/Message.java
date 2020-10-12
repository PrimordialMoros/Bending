/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.bending.model.user.User;
import me.moros.bending.util.AdventureUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.command.CommandSender;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;

/**
 * WIP Locale support
 * TODO: move to json resource file, import as locale and register in adventure
 */
public interface Message {
	Component PREFIX = text("[", DARK_GRAY)
		.append(text("Bending", DARK_AQUA))
		.append(text("] ", DARK_GRAY));

	Args0 HELP_HEADER = () -> brand(text("Help", DARK_AQUA));

	Args0 EMPTY_PRESET = () -> text("You can't create an empty preset!", YELLOW);
	Args0 NO_PRESETS = () -> text("No presets found", YELLOW);

	Args1<String> PRESET_SUCCESS = preset -> text("Successfully created preset {preset}", GREEN)
		.replaceFirstText("{preset}", text(preset));
	Args1<String> PRESET_EXISTS = preset -> text("Preset {preset} already exists!", YELLOW)
		.replaceFirstText("{preset}", text(preset));
	Args1<String> PRESET_FAIL = preset -> text("There was an error while saving preset {preset}", RED)
		.replaceFirstText("{preset}", text(preset));

	Args1<String> PRESET_REMOVE_SUCCESS = preset -> text("Preset {preset} has been removed", GREEN)
		.replaceFirstText("{preset}", text(preset));
	Args1<String> PRESET_REMOVE_FAIL = preset -> text("Failed to remove preset {preset}", RED)
		.replaceFirstText("{preset}", text(preset));

	Args2<Integer, String> PRESET_BIND_SUCCESS = (amount, preset) -> text("Successfully bound {amount} abilities from preset {preset}", GREEN)
		.replaceFirstText("{amount}", text(amount))
		.replaceFirstText("{preset}", text(preset));
	Args1<String> PRESET_BIND_FAIL = preset -> text("No abilities could be bound from preset {preset}", YELLOW);

	Args1<String> MODIFIER_ADD = name -> text("Successfully added modifier to {name}", GREEN)
		.replaceFirstText("{name}", text(name));
	Args1<String> MODIFIER_CLEAR = name -> text("Cleared attribute modifiers for {name}", GREEN)
		.replaceFirstText("{name}", text(name));

	Args0 TOGGLE_ON = () -> text("Your bending has been toggled back on.", GREEN);
	Args0 TOGGLE_OFF = () -> text("Your bending has been toggled back off.", RED);

	Args0 CONFIG_RELOAD = () -> text("Bending config reloaded", GREEN);

	Args1<Component> ELEMENT_CHOOSE_NO_PERMISSION = element -> text("You don't have permission to choose the element of {element}", RED)
		.replaceFirstText("{element}", element);
	Args1<Component> ELEMENT_CHOOSE_SUCCESS = element -> text("Your bending was set to the element of {element}", GREEN)
		.replaceFirstText("{element}", element);
	Args1<Component> ELEMENT_CHOOSE_FAIL = element -> text("Failed to choose element {element}", YELLOW)
		.replaceFirstText("{element}", element);

	Args1<Component> ELEMENT_ADD_NO_PERMISSION = element -> text("You don't have permission to add the element of {element}", RED)
		.replaceFirstText("{element}", element);
	Args1<Component> ELEMENT_ADD_SUCCESS = element -> text("You now have the element of {element}", GREEN)
		.replaceFirstText("{element}", element);
	Args1<Component> ELEMENT_ADD_FAIL = element -> text("You already have the element of {element}", YELLOW)
		.replaceFirstText("{element}", element);

	Args1<Component> ELEMENT_REMOVE_SUCCESS = element -> text("You no longer have the element of {element}", GRAY)
		.replaceFirstText("{element}", element);
	Args1<Component> ELEMENT_REMOVE_FAIL = element -> text("Failed to remove the element of {element}", YELLOW)
		.replaceFirstText("{element}", element);

	Args0 BOARD_DISABLED = () -> text("Bending Board is disabled!", RED);
	Args0 BOARD_TOGGLED_ON = () -> text("Toggled Bending Board on", GREEN);
	Args0 BOARD_TOGGLED_OFF = () -> text("Toggled Bending Board off", YELLOW);

	Args1<Component> ELEMENT_ABILITIES_EMPTY = element -> text("No abilities found for {element}", YELLOW)
		.replaceFirstText("{element}", element);

	Args0 ABILITIES = () -> text("Abilities:", DARK_GRAY);
	Args0 SEQUENCES = () -> text("Sequences:", DARK_GRAY);
	Args0 PASSIVES = () -> text("Passives:", DARK_GRAY);

	Args2<Component, Component> ABILITY_BIND_REQUIRES_ELEMENT = (ability, element) -> text("{ability} requires element {element}", YELLOW)
		.replaceFirstText("{ability}", ability)
		.replaceFirstText("{element}", element);

	Args1<Component> ABILITY_BIND_SEQUENCE = ability -> text("{ability} is a sequence and cannot be bound to a slot", YELLOW)
		.replaceFirstText("{ability}", ability);

	Args2<Component, Integer> ABILITY_BIND_SUCCESS = (ability, slot) -> text("Successfully bound {ability} to slot {slot}", GREEN)
		.replaceFirstText("{ability}", ability)
		.replaceFirstText("{slot}", text(slot));

	Args1<String> BOUND_SLOTS = name -> text("{name}'s bound abilities: ", DARK_AQUA)
		.replaceFirstText("{name}", text(name));

	Args0 CLEAR_ALL_SLOTS = () -> text("Cleared all slots:", GREEN);
	Args1<Integer> CLEAR_SLOT = slot -> text("Cleared ability slot {slot}: ", GREEN)
		.replaceFirstText("{slot}", text(slot));

	Args1<Component> ABILITY_INFO_EMPTY = ability -> text("No description or instructions found for {ability}", YELLOW)
		.replaceFirstText("{ability}", ability);

	Args2<Component, String> ABILITY_INFO_DESCRIPTION = (ability, details) -> text("{ability} description: {details}", GRAY)
		.replaceFirstText("{ability}", ability)
		.replaceFirstText("{details}", text(details));

	Args2<Component, String> ABILITY_INFO_INSTRUCTIONS = (ability, details) -> text("{ability} instructions: {details}", GRAY)
		.replaceFirstText("{ability}", ability)
		.replaceFirstText("{details}", text(details));

	static Component brand(String message) {
		return brand(text(message));
	}

	static Component brand(ComponentLike message) {
		return PREFIX.asComponent().append(message);
	}

	interface Args0 {
		Component build();

		default void send(CommandSender sender) {
			AdventureUtil.sendMessage(sender, build());
		}

		default void send(User user) {
			user.sendMessage(build());
		}
	}

	interface Args1<A0> {
		Component build(A0 arg0);

		default void send(CommandSender sender, A0 arg0) {
			AdventureUtil.sendMessage(sender, build(arg0));
		}

		default void send(User user, A0 arg0) {
			user.sendMessage(build(arg0));
		}
	}

	interface Args2<A0, A1> {
		Component build(A0 arg0, A1 arg1);

		default void send(CommandSender sender, A0 arg0, A1 arg1) {
			AdventureUtil.sendMessage(sender, build(arg0, arg1));
		}

		default void send(User user, A0 arg0, A1 arg1) {
			user.sendMessage(build(arg0, arg1));
		}
	}
}
