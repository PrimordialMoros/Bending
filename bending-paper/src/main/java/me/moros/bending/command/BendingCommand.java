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
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.moros.bending.Bending;
import me.moros.bending.game.Game;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.exception.command.UserException;
import me.moros.bending.model.predicates.conditionals.BendingConditions;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.ChatUtil;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CommandAlias("%bendingcommand")
public class BendingCommand extends BaseCommand {
	@HelpCommand
	@CommandPermission("bending.command.help")
	public static void doHelp(CommandSender sender, CommandHelp help) {
		ChatUtil.sendMessage(sender, ChatUtil.brand("Help"));
		help.showHelp();
	}

	@Subcommand("toggle|t")
	@CommandCompletion("@players")
	@CommandPermission("bending.command.toggle")
	@Description("Toggles bending")
	public void onToggle(BendingPlayer player, @Optional @CommandPermission("bending.command.toggle.others") OnlinePlayer target) {
		BendingPlayer bendingPlayer = target == null ? player : Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		if (bendingPlayer.getBendingConditional().hasConditional(BendingConditions.TOGGLED)) {
			bendingPlayer.getBendingConditional().remove(BendingConditions.TOGGLED);
			bendingPlayer.sendMessageKyori(TextComponent.of("Your bending has been toggled back on.", NamedTextColor.GREEN));
		} else {
			bendingPlayer.getBendingConditional().add(BendingConditions.TOGGLED);
			bendingPlayer.sendMessageKyori(TextComponent.of("Your bending has been toggled back off.", NamedTextColor.RED));
		}
	}

	@Subcommand("reload")
	@CommandPermission("bending.command.reload")
	@Description("Reloads the plugin and its config")
	public void onReload(CommandSender sender) {
		Game.reload();
		ChatUtil.sendMessage(sender, TextComponent.of("Bending config reloaded.", NamedTextColor.GREEN));
	}

	@Subcommand("choose|ch")
	@CommandPermission("bending.command.choose")
	@CommandCompletion("@elements @players")
	@Description("Choose an element")
	public static void onElementChoose(CommandSender sender, Element element, @Optional @CommandPermission("bending.command.choose.other") OnlinePlayer target) {
		if (target == null && !(sender instanceof Player)) throw new UserException("You must be player!");
		BendingPlayer player = Game.getPlayerManager().getPlayer((target == null) ? ((Player) sender).getUniqueId() : target.getPlayer().getUniqueId());
		if (target == null && !player.hasPermission("bending.command.choose." + element)) {
			player.sendMessageKyori(TextComponent.of(
				"You don't have permission to choose the element of ", NamedTextColor.RED)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
			return;
		}
		if (player.setElement(element)) {
			player.sendMessageKyori(TextComponent.of(
				"Your bending was set to the element of ", NamedTextColor.GRAY)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		} else {
			player.sendMessageKyori(TextComponent.of(
				"Failed to choose element ", NamedTextColor.YELLOW)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		}
	}

	@Subcommand("add|a")
	@CommandPermission("bending.command.add")
	@CommandCompletion("@elements @players")
	@Description("Add an element")
	public static void onElementAdd(CommandSender sender, Element element, @Optional @CommandPermission("bending.command.add.other") OnlinePlayer target) {
		if (target == null && !(sender instanceof Player)) throw new UserException("You must be player!");
		BendingPlayer player = Game.getPlayerManager().getPlayer((target == null) ? ((Player) sender).getUniqueId() : target.getPlayer().getUniqueId());
		if (target == null && !player.hasPermission("bending.command.add." + element)) {
			player.sendMessageKyori(TextComponent.of(
				"You don't have permission to add the element of ", NamedTextColor.RED)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
			return;
		}
		if (player.addElement(element)) {
			Game.getAbilityManager(player.getWorld()).clearPassives(player);
			Game.getAbilityManager(player.getWorld()).createPassives(player);
			player.sendMessageKyori(TextComponent.of(
				"You now have the element of ", NamedTextColor.GRAY)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		} else {
			player.sendMessageKyori(TextComponent.of(
				"You already have the element of ", NamedTextColor.YELLOW)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		}
	}

	@Subcommand("remove|rm|r|delete|del")
	@CommandPermission("bending.command.remove")
	@CommandCompletion("@elements @players")
	@Description("Remove an element")
	public static void onElementRemove(CommandSender sender, Element element, @Optional @CommandPermission("bending.command.remove.other") OnlinePlayer target) {
		if (target == null && !(sender instanceof Player)) throw new UserException("You must be player!");
		BendingPlayer player = Game.getPlayerManager().getPlayer((target == null) ? ((Player) sender).getUniqueId() : target.getPlayer().getUniqueId());
		if (player.removeElement(element)) {
			Game.getAbilityManager(player.getWorld()).clearPassives(player);
			Game.getAbilityManager(player.getWorld()).createPassives(player);
			player.sendMessageKyori(TextComponent.of(
				"You no longer have the element of ", NamedTextColor.GRAY)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		} else {
			player.sendMessageKyori(TextComponent.of(
				"Failed to remove the element of ", NamedTextColor.YELLOW)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		}
	}

	@Subcommand("bendingboard|board|bb")
	@CommandPermission("bending.command.board")
	@Description("Toggle bending board visibility")
	public static void onBoard(BendingPlayer player) {
		if (Game.isDisabledWorld(player.getWorld().getUID())) {
			player.sendMessageKyori(TextComponent.of("Bending Board is disabled!", NamedTextColor.RED));
			return;
		}
		if (Game.getBoardManager().toggleScoreboard(player.getEntity())) {
			player.sendMessageKyori(TextComponent.of("Toggled Bending Board on", NamedTextColor.GREEN));
			player.getProfile().setBoard(true);
		} else {
			player.sendMessageKyori(TextComponent.of("Toggled Bending Board off", NamedTextColor.YELLOW));
			player.getProfile().setBoard(false);
		}
	}

	@Subcommand("version|ver|v")
	@CommandPermission("bending.command.help")
	@Description("View version info about the bending plugin")
	public static void onVersion(CommandSender sender) {
		ChatUtil.sendMessage(sender, getVersionInfo());
	}


	@Subcommand("display|d|elements|element|elem|e")
	@CommandPermission("bending.command.display")
	@CommandCompletion("@elements")
	@Description("List all available abilities for a specific element")
	public static void onDisplay(CommandSender sender, Element element) {
		// TODO Implement paginator
		List<TextComponent> output = new ArrayList<>(16);
		List<TextComponent> normal = Game.getAbilityRegistry().getAbilities()
			.filter(d -> d.getElement() == element)
			.filter(Commands.getAbilityPredicate(Commands.FilterType.NORMAL))
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!normal.isEmpty()) {
			output.add(TextComponent.of("Abilities:", NamedTextColor.DARK_GRAY));
			output.addAll(normal);
		}

		List<TextComponent> sequences = Game.getSequenceManager().getRegisteredSequences()
			.filter(d -> d.getElement() == element)
			.filter(desc -> !desc.isHidden())
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!sequences.isEmpty()) {
			output.add(TextComponent.of("Sequences:", NamedTextColor.DARK_GRAY));
			output.addAll(sequences);
		}

		List<TextComponent> passives = Game.getAbilityRegistry().getPassives(element)
			.filter(desc -> !desc.isHidden())
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!passives.isEmpty()) {
			output.add(TextComponent.of("Passives:", NamedTextColor.DARK_GRAY));
			output.addAll(passives);
		}

		if (output.isEmpty()) {
			ChatUtil.sendMessage(sender, TextComponent.of(
				"No abilities found for ", NamedTextColor.DARK_GRAY).append(element.getDisplayName())
			);
			return;
		}
		output.forEach(text -> ChatUtil.sendMessage(sender, text));
	}

	@Subcommand("bind|b")
	@CommandPermission("bending.command.bind")
	@CommandCompletion("@abilities @range:1-9")
	@Description("Bind an ability to a slot")
	public static void onBind(BendingPlayer player, @Flags("filter=NORMAL") AbilityDescription ability, @Default("0") @Conditions("slot") Integer slot) {
		if (!player.hasElement(ability.getElement())) {
			player.sendMessageKyori(ability.getDisplayName()
				.append(TextComponent.of(" requires element ", NamedTextColor.YELLOW))
				.append(TextComponent.of(ability.getElement().toString(), ability.getElement().getColor()))
			);
			return;
		}
		if (slot == 0) slot = player.getHeldItemSlot();
		player.setSlotAbility(slot, ability);
		player.sendMessageKyori(ability.getDisplayName()
			.append(TextComponent.of(" was bound to slot " + slot, NamedTextColor.GREEN))
		);
	}

	@Subcommand("binds")
	@CommandPermission("bending.command.help")
	@CommandCompletion("@players")
	@Description("Show all bound abilities")
	public static void onBinds(BendingPlayer player, @Optional OnlinePlayer target) {
		BendingPlayer bendingPlayer = target == null ? player : Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		player.sendMessageKyori(TextComponent.of(bendingPlayer.getEntity().getName() + "'s bound abilities: ", NamedTextColor.DARK_AQUA));
		IntStream.rangeClosed(1, 9).forEach(slot -> bendingPlayer.getStandardSlotAbility(slot)
			.ifPresent(desc -> player.sendMessageKyori(
				TextComponent.of(slot + ". ", NamedTextColor.DARK_AQUA).append(AbilityDescription.getMeta(desc))
			))
		);
	}

	@Subcommand("clear|c")
	@CommandPermission("bending.command.bind")
	@CommandCompletion("@range:1-9")
	@Description("Clear an ability slot")
	public static void onClearBind(BendingPlayer player, @Default("0") @Conditions("slot") Integer slot) {
		if (slot == 0) {
			player.bindPreset(Preset.EMPTY);
			player.sendMessageKyori(TextComponent.of("Cleared all slots.", NamedTextColor.GREEN));
			return;
		}
		player.setSlotAbility(slot, null);
		player.sendMessageKyori(TextComponent.of("Cleared ability slot " + slot, NamedTextColor.GREEN));
	}

	@Subcommand("info|i")
	@CommandPermission("bending.command.help")
	@CommandCompletion("@abilities")
	@Description("View info about a specific ability")
	public static void onInfo(CommandSender sender, AbilityDescription ability) {
		String description = ability.getDescription();
		String instructions = ability.getInstructions();
		if (description.isEmpty() && instructions.isEmpty()) {
			ChatUtil.sendMessage(sender, TextComponent.of(
				"No description or instructions found for ", NamedTextColor.YELLOW)
				.append(ability.getDisplayName())
			);
		} else {
			if (!description.isEmpty()) {
				ChatUtil.sendMessage(sender, ability.getDisplayName()
					.append(TextComponent.of(" description: " + description, NamedTextColor.GRAY))
				);
			}
			if (!instructions.isEmpty()) {
				ChatUtil.sendMessage(sender, ability.getDisplayName()
					.append(TextComponent.of(" instructions: " + description, NamedTextColor.GRAY))
				);
			}
		}

		if (ability.isActivatedBy(ActivationMethod.SEQUENCE)) {
			Game.getSequenceManager().getSequence(ability).ifPresent(sequence -> ChatUtil.sendMessage(sender, ability.getDisplayName()
				.append(TextComponent.of(": " + sequence.getInstructions(), NamedTextColor.DARK_GRAY))
			));
		}
	}

	private static TextComponent getVersionInfo() {
		String link = "https://github.com/PrimordialMoros/Bending";
		TextComponent details = TextComponent.builder("Developed by: ", NamedTextColor.DARK_AQUA)
			.append(Bending.getAuthor(), NamedTextColor.GREEN).append(TextComponent.newline())
			.append("Source code: ", NamedTextColor.DARK_AQUA)
			.append(link, NamedTextColor.GREEN).append(TextComponent.newline())
			.append("Licensed under: ", NamedTextColor.DARK_AQUA)
			.append("AGPLv3", NamedTextColor.GREEN).append(TextComponent.newline()).append(TextComponent.newline())
			.append("Click to open link.", NamedTextColor.GRAY)
			.build();

		return TextComponent.builder("Version: ", NamedTextColor.DARK_AQUA)
			.append(Bending.getVersion(), NamedTextColor.GREEN)
			.hoverEvent(HoverEvent.of(HoverEvent.Action.SHOW_TEXT, details))
			.clickEvent(ClickEvent.of(ClickEvent.Action.OPEN_URL, link))
			.build();
	}
}
