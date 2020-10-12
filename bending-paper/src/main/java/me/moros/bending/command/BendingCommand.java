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
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.moros.bending.Bending;
import me.moros.bending.game.Game;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.exception.command.UserException;
import me.moros.bending.model.predicates.conditionals.BendingConditions;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.util.AdventureUtil;
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
		Message.HELP_HEADER.send(sender);
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
			Message.TOGGLE_ON.send(bendingPlayer);
		} else {
			bendingPlayer.getBendingConditional().add(BendingConditions.TOGGLED);
			Message.TOGGLE_OFF.send(bendingPlayer);
		}
	}

	@Subcommand("reload")
	@CommandPermission("bending.command.reload")
	@Description("Reloads the plugin and its config")
	public void onReload(CommandSender sender) {
		Game.reload();
		Message.CONFIG_RELOAD.send(sender);
	}

	@Subcommand("choose|ch")
	@CommandPermission("bending.command.choose")
	@CommandCompletion("@elements @players")
	@Description("Choose an element")
	public static void onElementChoose(CommandSender sender, Element element, @Optional @CommandPermission("bending.command.choose.other") OnlinePlayer target) {
		if (target == null && !(sender instanceof Player)) throw new UserException("You must be player!");
		BendingPlayer player = Game.getPlayerManager().getPlayer((target == null) ? ((Player) sender).getUniqueId() : target.getPlayer().getUniqueId());
		if (target == null && !player.hasPermission("bending.command.choose." + element)) {
			Message.ELEMENT_CHOOSE_NO_PERMISSION.send(player, element.getDisplayName());
			return;
		}
		if (player.setElement(element)) {
			Message.ELEMENT_CHOOSE_SUCCESS.send(player, element.getDisplayName());
		} else {
			Message.ELEMENT_CHOOSE_FAIL.send(player, element.getDisplayName());
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
			Message.ELEMENT_ADD_NO_PERMISSION.send(player, element.getDisplayName());
			return;
		}
		if (player.addElement(element)) {
			Game.getAbilityManager(player.getWorld()).clearPassives(player);
			Game.getAbilityManager(player.getWorld()).createPassives(player);
			Message.ELEMENT_ADD_SUCCESS.send(player, element.getDisplayName());
		} else {
			Message.ELEMENT_ADD_FAIL.send(player, element.getDisplayName());
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
			Message.ELEMENT_REMOVE_SUCCESS.send(player, element.getDisplayName());
		} else {
			Message.ELEMENT_REMOVE_FAIL.send(player, element.getDisplayName());
		}
	}

	@Subcommand("bendingboard|board|bb")
	@CommandPermission("bending.command.board")
	@Description("Toggle bending board visibility")
	public static void onBoard(BendingPlayer player) {
		if (Game.isDisabledWorld(player.getWorld().getUID())) {
			Message.BOARD_DISABLED.send(player);
			return;
		}
		if (Game.getBoardManager().toggleScoreboard(player.getEntity())) {
			player.getProfile().setBoard(true);
			Message.BOARD_TOGGLED_ON.send(player);
		} else {
			player.getProfile().setBoard(false);
			Message.BOARD_TOGGLED_OFF.send(player);
		}
	}

	@Subcommand("version|ver|v")
	@CommandPermission("bending.command.help")
	@Description("View version info about the bending plugin")
	public static void onVersion(CommandSender sender) {
		AdventureUtil.sendMessage(sender, getVersionInfo());
	}


	@Subcommand("display|d|elements|element|elem|e")
	@CommandPermission("bending.command.display")
	@CommandCompletion("@elements")
	@Description("List all available abilities for a specific element")
	public static void onDisplay(CommandSender sender, Element element) {
		// TODO Implement paginator
		List<Component> output = new ArrayList<>(16);
		List<Component> normal = Game.getAbilityRegistry().getAbilities()
			.filter(desc -> desc.getElement() == element)
			.filter(desc -> !desc.isHidden())
			.filter(desc -> !desc.isActivatedBy(ActivationMethod.SEQUENCE))
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!normal.isEmpty()) {
			output.add(Message.ABILITIES.build());
			output.addAll(normal);
		}

		List<Component> sequences = Game.getSequenceManager().getRegisteredSequences()
			.filter(d -> d.getElement() == element)
			.filter(desc -> !desc.isHidden())
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!sequences.isEmpty()) {
			output.add(Message.SEQUENCES.build());
			output.addAll(sequences);
		}

		List<Component> passives = Game.getAbilityRegistry().getPassives(element)
			.filter(desc -> !desc.isHidden())
			.filter(desc -> sender.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!passives.isEmpty()) {
			output.add(Message.PASSIVES.build());
			output.addAll(passives);
		}

		if (output.isEmpty()) {
			Message.ELEMENT_ABILITIES_EMPTY.send(sender, element.getDisplayName());
			return;
		}
		output.forEach(text -> AdventureUtil.sendMessage(sender, text));
	}

	@Subcommand("bind|b")
	@CommandPermission("bending.command.bind")
	@CommandCompletion("@abilities @range:1-9")
	@Description("Bind an ability to a slot")
	public static void onBind(BendingPlayer player, AbilityDescription ability, @Default("0") @Conditions("slot") Integer slot) {
		if (!player.hasElement(ability.getElement())) {
			Message.ABILITY_BIND_REQUIRES_ELEMENT.send(player, ability.getDisplayName(), ability.getElement().getDisplayName());
			return;
		}
		if (ability.isActivatedBy(ActivationMethod.SEQUENCE)) {
			Message.ABILITY_BIND_SEQUENCE.send(player, ability.getDisplayName());
			return;
		}
		if (slot == 0) slot = player.getHeldItemSlot();
		player.setSlotAbility(slot, ability);
		Message.ABILITY_BIND_SUCCESS.send(player, ability.getDisplayName(), slot);
	}

	@Subcommand("binds")
	@CommandPermission("bending.command.help")
	@CommandCompletion("@players")
	@Description("Show all bound abilities")
	public static void onBinds(BendingPlayer player, @Optional OnlinePlayer target) {
		BendingPlayer bendingPlayer = target == null ? player : Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		Message.BOUND_SLOTS.send(player, bendingPlayer.getEntity().getName());
		IntStream.rangeClosed(1, 9).forEach(slot -> bendingPlayer.getStandardSlotAbility(slot)
			.ifPresent(desc -> player.sendMessage(
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
			Message.CLEAR_ALL_SLOTS.send(player);
			return;
		}
		player.setSlotAbility(slot, null);
		Message.CLEAR_SLOT.send(player, slot);
	}

	@Subcommand("info|i")
	@CommandPermission("bending.command.help")
	@CommandCompletion("@abilities")
	@Description("View info about a specific ability")
	public static void onInfo(CommandSender sender, AbilityDescription ability) {
		String description = ability.getDescription();
		String instructions = ability.getInstructions();
		if (description.isEmpty() && instructions.isEmpty()) {
			Message.ABILITY_INFO_EMPTY.send(sender, ability.getDisplayName());
		} else {
			if (!description.isEmpty()) {
				Message.ABILITY_INFO_DESCRIPTION.send(sender, ability.getDisplayName(), description);
			}
			if (!instructions.isEmpty()) {
				Message.ABILITY_INFO_INSTRUCTIONS.send(sender, ability.getDisplayName(), instructions);
			}
		}

		if (ability.isActivatedBy(ActivationMethod.SEQUENCE)) {
			Game.getSequenceManager().getSequence(ability).ifPresent(sequence -> AdventureUtil.sendMessage(sender, ability.getDisplayName()
				.append(Component.text(": " + sequence.getInstructions(), NamedTextColor.DARK_GRAY))
			));
		}
	}

	private static Component getVersionInfo() {
		String link = "https://github.com/PrimordialMoros/Bending";
		String content = "Developed by: {author}\n" + "Source code: {link}\n" + "Licensed under: AGPLv3\n\n" + "Click to open link.";
		Component details = Component.text(content, NamedTextColor.DARK_AQUA)
			.replaceFirstText("{author}", Component.text(Bending.getAuthor(), NamedTextColor.GREEN))
			.replaceFirstText("{link}", Component.text(link, NamedTextColor.GREEN));

		return Component.text("Version: ", NamedTextColor.DARK_AQUA)
			.append(Component.text(Bending.getVersion(), NamedTextColor.GREEN))
			.hoverEvent(HoverEvent.showText(details))
			.clickEvent(ClickEvent.openUrl(link));
	}
}
