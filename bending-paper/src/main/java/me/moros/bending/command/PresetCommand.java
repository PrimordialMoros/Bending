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

package me.moros.bending.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.BendingCommandIssuer;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.util.ChatUtil;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;

@CommandAlias("%presetcommand")
@CommandPermission("bending.command.preset")
public class PresetCommand extends BaseCommand {
	@Default
	@Subcommand("list|ls|l")
	@Description("List all available presets")
	public static void onPresetList(BendingCommandIssuer sender) {
		Collection<String> presets = sender.getBendingPlayer().getPresets();
		if (presets.isEmpty()) {
			sender.sendMessageKyori(TextComponent.of("No presets found", NamedTextColor.YELLOW));
		} else {
			presets.forEach(sender::sendMessageKyori);
		}
	}

	@HelpCommand
	@CommandPermission("bending.command.help")
	public static void doHelp(BendingCommandIssuer sender, CommandHelp help) {
		sender.sendMessageKyori(ChatUtil.brand("Help"));
		help.showHelp();
	}

	@Subcommand("create|c")
	@Description("Create a new preset")
	public static void onPresetCreate(BendingCommandIssuer sender, String name) {
		String input = ChatUtil.sanitizeInput(name);
		BendingPlayer bendingPlayer = sender.getBendingPlayer();
		Preset preset = bendingPlayer.createPresetFromSlots(input);

		if (preset.isEmpty()) {
			sender.sendMessageKyori(TextComponent.of("You can't create an empty preset!", NamedTextColor.YELLOW));
			return;
		}

		bendingPlayer.addPreset(preset, result -> {
			switch (result) {
				case EXISTS:
					sender.sendMessageKyori(TextComponent.of("Preset " + input + " already exists!", NamedTextColor.YELLOW));
					break;
				case FAIL:
					sender.sendMessageKyori(TextComponent.of("There was an error while saving preset " + input, NamedTextColor.RED));
					break;
				case SUCCESS:
				default:
					sender.sendMessageKyori(TextComponent.of("Successfully created preset " + input, NamedTextColor.GREEN));
					break;
			}
		});
	}

	@Subcommand("remove|rm|r|delete|del|d")
	@CommandCompletion("@presets")
	@Description("Remove an existing preset")
	public static void onPresetRemove(BendingCommandIssuer sender, Preset preset) {
		if (sender.getBendingPlayer().removePreset(preset)) {
			sender.sendMessageKyori(TextComponent.of("Preset " + preset.getName() + " has been removed", NamedTextColor.GREEN));
		} else {
			sender.sendMessageKyori(TextComponent.of("Failed to remove preset " + preset.getName(), NamedTextColor.RED));
		}
	}

	@Subcommand("bind|b")
	@CommandCompletion("@presets")
	@Description("Bind an existing preset")
	public static void onPresetBind(BendingCommandIssuer sender, Preset preset) {
		int count = sender.getBendingPlayer().bindPreset(preset);
		if (count > 0) {
			sender.sendMessageKyori(TextComponent.of(count + " abilities were bound from the preset", NamedTextColor.GREEN));
		} else {
			sender.sendMessageKyori(TextComponent.of("No abilities could be bound from preset " + preset.getName(), NamedTextColor.YELLOW));
		}
	}
}
