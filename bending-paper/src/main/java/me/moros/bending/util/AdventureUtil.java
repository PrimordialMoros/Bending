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
import org.bukkit.command.CommandSender;

/**
 * Utility class to handle any functionality related to <a href="https://github.com/KyoriPowered/adventure">Adventure Framework</a>.
 */
public final class AdventureUtil {
	/**
	 * Sends a chat message to the provided sender.
	 * @param sender the message receiver
	 * @param text the text message as a Component
	 */
	public static void sendMessage(CommandSender sender, Component text) {
		Bending.getAudiences().sender(sender).sendMessage(text);
	}

	/**
	 * Sends an ActionBar message to the provided sender.
	 * @param sender the message receiver
	 * @param text the text message as a Component
	 */
	public static void sendActionBar(CommandSender sender, Component text) {
		Bending.getAudiences().sender(sender).sendActionBar(text);
	}
}
