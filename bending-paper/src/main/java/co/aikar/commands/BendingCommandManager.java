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

package co.aikar.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class BendingCommandManager extends PaperCommandManager {
	public BendingCommandManager(Plugin plugin) {
		super(plugin);
	}

	@Override
	public BendingCommandIssuer getCommandIssuer(Object issuer) {
		if (!(issuer instanceof CommandSender)) {
			throw new IllegalArgumentException(issuer.getClass().getName() + " is not a Command Issuer.");
		}
		return new BendingCommandIssuer(this, (CommandSender) issuer);
	}
}
