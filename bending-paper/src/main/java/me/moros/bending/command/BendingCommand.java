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
import net.kyori.adventure.text.Component;
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
			bendingPlayer.sendMessageKyori(Component.text("Your bending has been toggled back on.", NamedTextColor.GREEN));
		} else {
			bendingPlayer.getBendingConditional().add(BendingConditions.TOGGLED);
			bendingPlayer.sendMessageKyori(Component.text("Your bending has been toggled back off.", NamedTextColor.RED));
		}
	}

	@Subcommand("reload")
	@CommandPermission("bending.command.reload")
	@Description("Reloads the plugin and its config")
	public void onReload(CommandSender sender) {
		Game.reload();
		ChatUtil.sendMessage(sender, Component.text("Bending config reloaded.", NamedTextColor.GREEN));
	}

	@Subcommand("choose|ch")
	@CommandPermission("bending.command.choose")
	@CommandCompletion("@elements @players")
	@Description("Choose an element")
	public static void onElementChoose(CommandSender sender, Element element, @Optional @CommandPermission("bending.command.choose.other") OnlinePlayer target) {
		if (target == null && !(sender instanceof Player)) throw new UserException("You must be player!");
		BendingPlayer player = Game.getPlayerManager().getPlayer((target == null) ? ((Player) sender).getUniqueId() : target.getPlayer().getUniqueId());
		if (target == null && !player.hasPermission("bending.command.choose." + element)) {
			player.sendMessageKyori(Component.text(
				"You don't have permission to choose the element of ", NamedTextColor.RED)
				.append(Component.text(element.toString(), element.getColor()))
			);
			return;
		}
		if (player.setElement(element)) {
			player.sendMessageKyori(Component.text(
				"Your bending was set to the element of ", NamedTextColor.GRAY)
				.append(Component.text(element.toString(), element.getColor()))
			);
		} else {
			player.sendMessageKyori(Component.text(
				"Failed to choose element ", NamedTextColor.YELLOW)
				.append(Component.text(element.toString(), element.getColor()))
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
			player.sendMessageKyori(Component.text(
				"You don't have permission to add the element of ", NamedTextColor.RED)
				.append(Component.text(element.toString(), element.getColor()))
			);
			return;
		}
		if (player.addElement(element)) {
			Game.getAbilityManager(player.getWorld()).clearPassives(player);
			Game.getAbilityManager(player.getWorld()).createPassives(player);
			player.sendMessageKyori(Component.text(
				"You now have the element of ", NamedTextColor.GRAY)
				.append(Component.text(element.toString(), element.getColor()))
			);
		} else {
			player.sendMessageKyori(Component.text(
				"You already have the element of ", NamedTextColor.YELLOW)
				.append(Component.text(element.toString(), element.getColor()))
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
			player.sendMessageKyori(Component.text(
				"You no longer have the element of ", NamedTextColor.GRAY)
				.append(Component.text(element.toString(), element.getColor()))
			);
		} else {
			player.sendMessageKyori(Component.text(
				"Failed to remove the element of ", NamedTextColor.YELLOW)
				.append(Component.text(element.toString(), element.getColor()))
			);
		}
	}

	@Subcommand("bendingboard|board|bb")
	@CommandPermission("bending.command.board")
	@Description("Toggle bending board visibility")
	public static void onBoard(BendingPlayer player) {
		if (Game.isDisabledWorld(player.getWorld().getUID())) {
			player.sendMessageKyori(Component.text("Bending Board is disabled!", NamedTextColor.RED));
			return;
		}
		if (Game.getBoardManager().toggleScoreboard(player.getEntity())) {
			player.sendMessageKyori(Component.text("Toggled Bending Board on", NamedTextColor.GREEN));
			player.getProfile().setBoard(true);
		} else {
			player.sendMessageKyori(Component.text("Toggled Bending Board off", NamedTextColor.YELLOW));
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
		List<Component> output = new ArrayList<>(16);
		List<Component> normal = Game.getAbilityRegistry().getAbilities()
			.filter(d -> d.getElement() == element)
			.filter(Commands.getAbilityPredicate(Commands.FilterType.NORMAL))
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!normal.isEmpty()) {
			output.add(Component.text("Abilities:", NamedTextColor.DARK_GRAY));
			output.addAll(normal);
		}

		List<Component> sequences = Game.getSequenceManager().getRegisteredSequences()
			.filter(d -> d.getElement() == element)
			.filter(desc -> !desc.isHidden())
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!sequences.isEmpty()) {
			output.add(Component.text("Sequences:", NamedTextColor.DARK_GRAY));
			output.addAll(sequences);
		}

		List<Component> passives = Game.getAbilityRegistry().getPassives(element)
			.filter(desc -> !desc.isHidden())
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!passives.isEmpty()) {
			output.add(Component.text("Passives:", NamedTextColor.DARK_GRAY));
			output.addAll(passives);
		}

		if (output.isEmpty()) {
			ChatUtil.sendMessage(sender, Component.text(
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
				.append(Component.text(" requires element ", NamedTextColor.YELLOW))
				.append(Component.text(ability.getElement().toString(), ability.getElement().getColor()))
			);
			return;
		}
		if (slot == 0) slot = player.getHeldItemSlot();
		player.setSlotAbility(slot, ability);
		player.sendMessageKyori(ability.getDisplayName()
			.append(Component.text(" was bound to slot " + slot, NamedTextColor.GREEN))
		);
	}

	@Subcommand("binds")
	@CommandPermission("bending.command.help")
	@CommandCompletion("@players")
	@Description("Show all bound abilities")
	public static void onBinds(BendingPlayer player, @Optional OnlinePlayer target) {
		BendingPlayer bendingPlayer = target == null ? player : Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		player.sendMessageKyori(Component.text(bendingPlayer.getEntity().getName() + "'s bound abilities: ", NamedTextColor.DARK_AQUA));
		IntStream.rangeClosed(1, 9).forEach(slot -> bendingPlayer.getStandardSlotAbility(slot)
			.ifPresent(desc -> player.sendMessageKyori(
				Component.text(slot + ". ", NamedTextColor.DARK_AQUA).append(AbilityDescription.getMeta(desc))
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
			player.sendMessageKyori(Component.text("Cleared all slots.", NamedTextColor.GREEN));
			return;
		}
		player.setSlotAbility(slot, null);
		player.sendMessageKyori(Component.text("Cleared ability slot " + slot, NamedTextColor.GREEN));
	}

	@Subcommand("info|i")
	@CommandPermission("bending.command.help")
	@CommandCompletion("@abilities")
	@Description("View info about a specific ability")
	public static void onInfo(CommandSender sender, AbilityDescription ability) {
		String description = ability.getDescription();
		String instructions = ability.getInstructions();
		if (description.isEmpty() && instructions.isEmpty()) {
			ChatUtil.sendMessage(sender, Component.text(
				"No description or instructions found for ", NamedTextColor.YELLOW)
				.append(ability.getDisplayName())
			);
		} else {
			if (!description.isEmpty()) {
				ChatUtil.sendMessage(sender, ability.getDisplayName()
					.append(Component.text(" description: " + description, NamedTextColor.GRAY))
				);
			}
			if (!instructions.isEmpty()) {
				ChatUtil.sendMessage(sender, ability.getDisplayName()
					.append(Component.text(" instructions: " + description, NamedTextColor.GRAY))
				);
			}
		}

		if (ability.isActivatedBy(ActivationMethod.SEQUENCE)) {
			Game.getSequenceManager().getSequence(ability).ifPresent(sequence -> ChatUtil.sendMessage(sender, ability.getDisplayName()
				.append(Component.text(": " + sequence.getInstructions(), NamedTextColor.DARK_GRAY))
			));
		}
	}

	private static Component getVersionInfo() {
		String link = "https://github.com/PrimordialMoros/Bending";
		Component details = Component.text("Developed by: ", NamedTextColor.DARK_AQUA)
			.append(Component.text(Bending.getAuthor(), NamedTextColor.GREEN)).append(Component.newline())
			.append(Component.text("Source code: ", NamedTextColor.DARK_AQUA))
			.append(Component.text(link, NamedTextColor.GREEN)).append(Component.newline())
			.append(Component.text("Licensed under: ", NamedTextColor.DARK_AQUA))
			.append(Component.text("AGPLv3", NamedTextColor.GREEN)).append(Component.newline()).append(Component.newline())
			.append(Component.text("Click to open link.", NamedTextColor.GRAY));

		return Component.text("Version: ", NamedTextColor.DARK_AQUA)
			.append(Component.text(Bending.getVersion(), NamedTextColor.GREEN))
			.hoverEvent(HoverEvent.showText(details))
			.clickEvent(ClickEvent.openUrl(link));
	}
}
