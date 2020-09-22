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

import java.util.regex.Pattern;

/**
 * Utility class to handle chat related functionality.
 */
public class ChatUtil {
	private static final Pattern NON_ALPHABETICAL = Pattern.compile("[^A-Za-z]");
	/**
	 * @see #sendMessage(CommandSender, Component)
	 */
	public static void sendMessage(CommandSender receiver, String text) {
		sendMessage(receiver, TextComponent.of(text));
	}

	/**
	 * Sends a message to the provided sender using the <a href="https://github.com/KyoriPowered/adventure">Adventure</a> Framework.
	 * @param receiver the message receiver
	 * @param text the text message as a Component
	 */
	public static void sendMessage(CommandSender receiver, Component text) {
		Bending.getAudiences().audience(receiver).sendMessage(text);
	}

	public static TextComponent brand(String text) {
		return brand(TextComponent.of(text));
	}

	public static TextComponent brand(TextComponent text) {
		return Bending.PREFIX.append(text);
	}

	/**
	 * Strip input of all non alphabetical values and limit to 16 characters long.
	 * This is used for preset names mainly.
	 * @param input input the input string to sanitize
	 * @return the sanitized output string
	 */
	public static String sanitizeInput(String input) {
		String output = NON_ALPHABETICAL.matcher(input).replaceAll("").toLowerCase();
		return output.length() > 16 ? output.substring(0, 16) : output;
	}

	public static ChatColor getLegacyColor(TextColor color) {
		return ChatColor.valueOf(NamedTextColor.nearestTo(color).toString().toUpperCase());
	}
}
