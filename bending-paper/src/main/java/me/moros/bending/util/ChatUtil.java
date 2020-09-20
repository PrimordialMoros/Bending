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

package me.moros.bending.util;

import me.moros.bending.Bending;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ChatUtil {
	public static void sendMessage(CommandSender sender, String text) {
		sendMessage(sender, TextComponent.of(text));
	}

	public static void sendMessage(CommandSender sender, Component text) {
		Bending.getAudiences().audience(sender).sendMessage(text);
	}

	public static TextComponent brand(String text) {
		return brand(TextComponent.of(text));
	}

	public static TextComponent brand(TextComponent text) {
		return Bending.PREFIX.append(text);
	}

	/**
	 * Strip input of all non alphabetical values and limit to 16 characters long
	 *
	 * @param input input the input string to sanitize
	 * @return the sanitized output string
	 */
	public static String sanitizeInput(String input) {
		final String output = input.replaceAll("[^A-Za-z]", "").toLowerCase();
		return output.length() > 16 ? output.substring(0, 16) : output;
	}

	public static ChatColor getLegacyColor(TextColor color) {
		return ChatColor.valueOf(NamedTextColor.nearestTo(color).toString().toUpperCase());
	}
}
