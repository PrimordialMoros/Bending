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
import co.aikar.commands.annotation.Conditions;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import me.moros.bending.Bending;
import me.moros.bending.board.BoardManager;
import me.moros.bending.game.Game;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.AbilityAction;
import me.moros.bending.model.ability.sequence.Action;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.user.player.BendingPlayer;
import me.moros.bending.model.predicates.conditionals.BendingConditions;
import me.moros.bending.util.ChatUtil;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@CommandAlias("%bendingcommand")
public class BendingCommand extends BaseCommand {
	@HelpCommand
	@CommandPermission("bending.command.help")
	public static void doHelp(BendingCommandIssuer sender, CommandHelp help) {
		sender.sendMessageKyori(ChatUtil.brand("Help"));
		help.showHelp();
	}

	@Subcommand("toggle|t")
	@CommandCompletion("@players")
	@CommandPermission("bending.command.toggle")
	@Description("Toggles bending")
	public void onToggle(BendingCommandIssuer sender, @Optional @CommandPermission("bending.command.toggle.others") OnlinePlayer target) {
		BendingPlayer bendingPlayer;
		if (target != null) {
			bendingPlayer = Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		} else {
			bendingPlayer = sender.getBendingPlayer();
		}
		if (bendingPlayer.getBendingConditional().hasConditional(BendingConditions.TOGGLED)) {
			bendingPlayer.getBendingConditional().remove(BendingConditions.TOGGLED);
			ChatUtil.sendMessage(bendingPlayer, TextComponent.of("Your bending has been toggled back on.", NamedTextColor.GREEN));
		} else {
			bendingPlayer.getBendingConditional().add(BendingConditions.TOGGLED);
			ChatUtil.sendMessage(bendingPlayer, TextComponent.of("Your bending has been toggled back off.", NamedTextColor.RED));
		}
	}

	@Subcommand("reload")
	@CommandPermission("bending.command.reload")
	@Description("Reloads the plugin and its config")
	public void onReload(BendingCommandIssuer sender) {
		Game.reload();
		sender.sendMessageKyori(TextComponent.of("Bending config reloaded.", NamedTextColor.GREEN));
	}

	@Subcommand("choose|ch")
	@CommandPermission("bending.command.choose")
	@CommandCompletion("@elements *")
	@Description("Choose an element")
	public static void onElementChoose(BendingCommandIssuer sender, Element element, @Optional @CommandPermission("bending.command.choose.others") OnlinePlayer target) {
		if (!sender.hasPermission("bending.command.choose." + element)) {
			sender.sendMessageKyori(TextComponent.of(
				"You don't have permission to choose the element of ", NamedTextColor.RED)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
			return;
		}
		BendingPlayer bendingPlayer;
		if (target != null) {
			bendingPlayer = Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		} else {
			bendingPlayer = sender.getBendingPlayer();
		}
		if (bendingPlayer.setElement(element)) {
			ChatUtil.sendMessage(bendingPlayer, TextComponent.of(
				"Your bending was set to the element of ", NamedTextColor.GRAY)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		} else {
			sender.sendMessageKyori(TextComponent.of(
				"Failed to choose element ", NamedTextColor.YELLOW)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		}
	}

	@Subcommand("add|a")
	@CommandPermission("bending.command.add")
	@CommandCompletion("@elements @players")
	@Description("Add an element")
	public static void onElementAdd(BendingCommandIssuer sender, Element element, @Optional @CommandPermission("bending.command.add.others") OnlinePlayer target) {
		if (!sender.hasPermission("bending.command.add." + element)) {
			sender.sendMessageKyori(TextComponent.of(
				"You don't have permission to add the element of ", NamedTextColor.RED)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
			return;
		}
		BendingPlayer bendingPlayer;
		if (target != null) {
			bendingPlayer = Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		} else {
			bendingPlayer = sender.getBendingPlayer();
		}
		if (bendingPlayer.addElement(element)) {
			Game.getAbilityInstanceManager(bendingPlayer.getWorld()).clearPassives(bendingPlayer);
			Game.getAbilityInstanceManager(bendingPlayer.getWorld()).createPassives(bendingPlayer);
			ChatUtil.sendMessage(bendingPlayer, TextComponent.of(
				"You now have the element of ", NamedTextColor.GRAY)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		} else {
			sender.sendMessageKyori(TextComponent.of(
				"User already has the element of ", NamedTextColor.YELLOW)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		}
	}

	@Subcommand("remove|rm|r|delete|del")
	@CommandPermission("bending.command.add")
	@CommandCompletion("@elements @players")
	@Description("Remove an element")
	public static void onElementRemove(BendingCommandIssuer sender, Element element, @Optional @CommandPermission("bending.command.remove.others") OnlinePlayer target) {
		BendingPlayer bendingPlayer;
		if (target != null) {
			bendingPlayer = Game.getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		} else {
			bendingPlayer = sender.getBendingPlayer();
		}
		if (bendingPlayer.removeElement(element)) {
			Game.getAbilityInstanceManager(bendingPlayer.getWorld()).clearPassives(bendingPlayer);
			Game.getAbilityInstanceManager(bendingPlayer.getWorld()).createPassives(bendingPlayer);
			ChatUtil.sendMessage(bendingPlayer, TextComponent.of(
				"You no longer have the element of ", NamedTextColor.GRAY)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		} else {
			sender.sendMessageKyori(TextComponent.of(
				"User doesn't have the element of ", NamedTextColor.YELLOW)
				.append(TextComponent.of(element.toString(), element.getColor()))
			);
		}
	}

	@Subcommand("bendingboard|board|bb")
	@CommandPermission("bending.command.board")
	@Description("Toggle bending board visibility")
	public static void onBoard(BendingCommandIssuer sender) {
		if (Game.isDisabledWorld(sender.getPlayer().getWorld().getUID())) {
			sender.sendMessageKyori(TextComponent.of("Bending Board is disabled!", NamedTextColor.RED));
			return;
		}
		if (BoardManager.toggleScoreboard(sender.getPlayer())) {
			sender.sendMessageKyori(TextComponent.of("Toggled Bending Board on", NamedTextColor.GREEN));
			sender.getBendingPlayer().getProfile().setBoard(true);
		} else {
			sender.sendMessageKyori(TextComponent.of("Toggled Bending Board off", NamedTextColor.YELLOW));
			sender.getBendingPlayer().getProfile().setBoard(false);
		}
	}

	@Subcommand("version|ver|v")
	@CommandPermission("bending.command.help")
	@Description("View version info about the bending plugin")
	public static void onVersion(BendingCommandIssuer sender) {
		sender.sendMessageKyori(getVersionInfo());
	}

	@Subcommand("display|d|elements|element|elem|e")
	@CommandPermission("bending.command.display")
	public static class DisplayCommand extends BaseCommand {
		public DisplayCommand(BendingCommand command) {
		}

		@Default
		@CommandCompletion("@elements")
		@Description("List all available abilities for a specific element")
		public static void onDisplay(BendingCommandIssuer sender, Element element) {
			BendingPlayer bendingPlayer = sender.getBendingPlayer();
			// TODO Implement paginator

			Map<Commands.FilterType, List<AbilityDescription>> groups = Game.getAbilityRegistry().getAbilities()
				.filter(d -> d.getElement() == element)
				.filter(Commands.getAbilityPredicate())
				.filter(bendingPlayer::hasPermission)
				.collect(Collectors.groupingBy(Commands::getMainType));

			List<TextComponent> output = new ArrayList<>(16);
			List<TextComponent> normal = groups.getOrDefault(Commands.FilterType.NORMAL, Collections.emptyList()).stream()
				.map(AbilityDescription::getMeta)
				.collect(Collectors.toList());
			if (!normal.isEmpty()) {
				output.add(TextComponent.of("Abilities:", NamedTextColor.DARK_GRAY));
				output.addAll(normal);
			}

			List<TextComponent> sequences = groups.getOrDefault(Commands.FilterType.SEQUENCE, Collections.emptyList()).stream()
				.map(AbilityDescription::getMeta)
				.collect(Collectors.toList());

			if (!sequences.isEmpty()) {
				output.add(TextComponent.of("Sequences:", NamedTextColor.DARK_GRAY));
				output.addAll(sequences);
			}

			List<TextComponent> passives = groups.getOrDefault(Commands.FilterType.PASSIVE, Collections.emptyList()).stream()
				.map(AbilityDescription::getMeta)
				.collect(Collectors.toList());

			if (!passives.isEmpty()) {
				output.add(TextComponent.of("Passives:", NamedTextColor.DARK_GRAY));
				output.addAll(passives);
			}

			if (output.isEmpty()) {
				sender.sendMessageKyori(
					TextComponent.of("No abilities found for ", NamedTextColor.DARK_GRAY).append(element.getDisplayName())
				);
				return;
			}
			output.forEach(sender::sendMessageKyori);
		}
	}

	@Subcommand("bind|b")
	@CommandPermission("bending.command.bind")
	@CommandCompletion("@abilities @range:1-9")
	@Description("Bind an ability to a slot")
	public static void onBind(BendingCommandIssuer sender, @Flags("filter=NORMAL") AbilityDescription ability, @Default("0") @Conditions("slot") Integer slot) {
		if (!sender.getBendingPlayer().hasElement(ability.getElement())) {
			sender.sendMessageKyori(ability.getDisplayName()
				.append(TextComponent.of(" requires element ", NamedTextColor.YELLOW))
				.append(TextComponent.of(ability.getElement().toString(), ability.getElement().getColor()))
			);
			return;
		}
		if (slot == 0) slot = sender.getBendingPlayer().getHeldItemSlot();
		sender.getBendingPlayer().setSlotAbility(slot, ability);
		sender.sendMessageKyori(ability.getDisplayName()
			.append(TextComponent.of(" was bound to slot " + slot, NamedTextColor.GREEN))
		);
	}

	@Subcommand("binds")
	@CommandPermission("bending.command.help")
	@Description("Show all bound abilities")
	public static void onBinds(BendingCommandIssuer sender) {
		BendingPlayer bendingPlayer = sender.getBendingPlayer();
		sender.sendMessageKyori(TextComponent.of("Bound abilities: ", NamedTextColor.DARK_AQUA));
		IntStream.rangeClosed(1, 9).forEach(slot -> bendingPlayer.getStandardSlotAbility(slot)
			.ifPresent(desc -> sender.sendMessageKyori(
				TextComponent.of(slot + ". ", NamedTextColor.DARK_AQUA).append(AbilityDescription.getMeta(desc))
			))
		);
	}

	@Subcommand("clear|c")
	@CommandPermission("bending.command.bind")
	@CommandCompletion("@range:1-9")
	@Description("Clear an ability slot")
	public static void onClearBind(BendingCommandIssuer sender, @Default("0") @Conditions("slot") Integer slot) {
		if (slot == 0) slot = sender.getBendingPlayer().getHeldItemSlot();
		sender.getBendingPlayer().setSlotAbility(slot, null);
		sender.sendMessageKyori(TextComponent.of("Cleared ability slot " + slot, NamedTextColor.GREEN));
	}

	@Subcommand("info|i")
	@CommandPermission("bending.command.help")
	@CommandCompletion("@abilities")
	@Description("View info about a specific ability")
	public static void onInfo(BendingCommandIssuer sender, AbilityDescription ability) {
		String description = ability.getDescription();
		String instructions = ability.getInstructions();

		if (description.isEmpty() && instructions.isEmpty()) {
			sender.sendMessageKyori(
				TextComponent.of("No description or instructions found for ", NamedTextColor.YELLOW)
					.append(ability.getDisplayName())
			);
		} else {
			if (!description.isEmpty()) {
				sender.sendMessageKyori(ability.getDisplayName()
					.append(TextComponent.of(" description: " + description, NamedTextColor.GRAY))
				);
			}
			if (!instructions.isEmpty()) {
				sender.sendMessageKyori(ability.getDisplayName()
					.append(TextComponent.of(" instructions: " + description, NamedTextColor.GRAY))
				);
			}
		}

		if (ability.isActivatedBy(ActivationMethod.SEQUENCE)) {
			Sequence sequence = Game.getSequenceManager().getSequence(ability);
			if (sequence != null) {
				String sequenceInstructions = getSequenceInstructions(sequence);
				sender.sendMessageKyori(ability.getDisplayName()
					.append(TextComponent.of(" sequence: " + sequenceInstructions, NamedTextColor.DARK_GRAY))
				);
			}
		}
	}

	private static String getSequenceInstructions(Sequence sequence) {
		StringBuilder sb = new StringBuilder();
		List<AbilityAction> actions = sequence.getActions();
		for (int i = 0; i < actions.size(); ++i) {
			AbilityAction abilityAction = actions.get(i);
			if (i != 0) {
				sb.append(" > ");
			}
			AbilityDescription desc = abilityAction.getAbilityDescription();
			Action action = abilityAction.getAction();
			String actionString = action.toString();
			if (action == Action.SNEAK) {
				actionString = "Hold Sneak";
				// Check if the next instruction is to release this sneak.
				if (i + 1 < actions.size()) {
					AbilityAction next = actions.get(i + 1);
					if (next.getAbilityDescription() == desc && next.getAction() == Action.SNEAK_RELEASE) {
						actionString = "Tap Sneak";
						++i;
					}
				}
			}
			sb.append(desc.toString()).append(" (").append(actionString).append(")");
		}
		return sb.toString();
	}

	private static TextComponent getVersionInfo() {
		String link = "https://github.com/PrimordialMoros/"; // TODO add proper link
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
