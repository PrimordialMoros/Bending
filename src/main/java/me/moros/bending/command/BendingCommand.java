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

import me.moros.atlas.acf.BaseCommand;
import me.moros.atlas.acf.CommandHelp;
import me.moros.atlas.acf.annotation.CommandAlias;
import me.moros.atlas.acf.annotation.CommandCompletion;
import me.moros.atlas.acf.annotation.CommandPermission;
import me.moros.atlas.acf.annotation.Conditions;
import me.moros.atlas.acf.annotation.Default;
import me.moros.atlas.acf.annotation.Description;
import me.moros.atlas.acf.annotation.HelpCommand;
import me.moros.atlas.acf.annotation.Optional;
import me.moros.atlas.acf.annotation.Subcommand;
import me.moros.atlas.acf.bukkit.contexts.OnlinePlayer;
import me.moros.atlas.kyori.adventure.text.Component;
import me.moros.atlas.kyori.adventure.text.event.ClickEvent;
import me.moros.atlas.kyori.adventure.text.event.HoverEvent;
import me.moros.atlas.kyori.adventure.text.format.NamedTextColor;
import me.moros.bending.Bending;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.exception.command.UserException;
import me.moros.bending.model.predicates.conditionals.BendingConditions;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.CommandUser;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

@CommandAlias("%bendingcommand")
public class BendingCommand extends BaseCommand {
	@HelpCommand
	@CommandPermission("bending.command.help")
	public static void doHelp(CommandUser user, CommandHelp help) {
		Message.HELP_HEADER.send(user);
		help.showHelp();
	}

	@Subcommand("toggle|t")
	@CommandCompletion("@players")
	@CommandPermission("bending.command.toggle")
	@Description("Toggles bending")
	public void onToggle(BendingPlayer player, @Optional @CommandPermission("bending.command.toggle.others") OnlinePlayer target) {
		BendingPlayer bendingPlayer = target == null ? player : Bending.getGame().getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
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
	public void onReload(CommandUser user) {
		Bending.getGame().reload();
		Message.CONFIG_RELOAD.send(user);
	}

	@Subcommand("choose|ch")
	@CommandPermission("bending.command.choose")
	@CommandCompletion("@elements @players")
	@Description("Choose an element")
	public static void onElementChoose(CommandUser user, Element element, @Optional @CommandPermission("bending.command.choose.other") OnlinePlayer target) {
		if (target == null && !(user instanceof Player)) throw new UserException("You must be player!");
		BendingPlayer player = Bending.getGame().getPlayerManager().getPlayer((target == null) ? ((Player) user).getUniqueId() : target.getPlayer().getUniqueId());
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
	public static void onElementAdd(CommandUser user, Element element, @Optional @CommandPermission("bending.command.add.other") OnlinePlayer target) {
		if (target == null && !(user instanceof Player)) throw new UserException("You must be player!");
		BendingPlayer player = Bending.getGame().getPlayerManager().getPlayer((target == null) ? ((Player) user).getUniqueId() : target.getPlayer().getUniqueId());
		if (target == null && !player.hasPermission("bending.command.add." + element)) {
			Message.ELEMENT_ADD_NO_PERMISSION.send(player, element.getDisplayName());
			return;
		}
		if (player.addElement(element)) {
			Bending.getGame().getAbilityManager(player.getWorld()).clearPassives(player);
			Bending.getGame().getAbilityManager(player.getWorld()).createPassives(player);
			Message.ELEMENT_ADD_SUCCESS.send(player, element.getDisplayName());
		} else {
			Message.ELEMENT_ADD_FAIL.send(player, element.getDisplayName());
		}
	}

	@Subcommand("remove|rm|r|delete|del")
	@CommandPermission("bending.command.remove")
	@CommandCompletion("@elements @players")
	@Description("Remove an element")
	public static void onElementRemove(CommandUser user, Element element, @Optional @CommandPermission("bending.command.remove.other") OnlinePlayer target) {
		if (target == null && !(user instanceof Player)) throw new UserException("You must be player!");
		BendingPlayer player = Bending.getGame().getPlayerManager().getPlayer((target == null) ? ((Player) user).getUniqueId() : target.getPlayer().getUniqueId());
		if (player.removeElement(element)) {
			Bending.getGame().getAbilityManager(player.getWorld()).clearPassives(player);
			Bending.getGame().getAbilityManager(player.getWorld()).createPassives(player);
			Message.ELEMENT_REMOVE_SUCCESS.send(player, element.getDisplayName());
		} else {
			Message.ELEMENT_REMOVE_FAIL.send(player, element.getDisplayName());
		}
	}

	@Subcommand("bendingboard|board|bb")
	@CommandPermission("bending.command.board")
	@Description("Toggle bending board visibility")
	public static void onBoard(BendingPlayer player) {
		if (Bending.getGame().isDisabledWorld(player.getWorld().getUID())) {
			Message.BOARD_DISABLED.send(player);
			return;
		}
		if (Bending.getGame().getBoardManager().toggleScoreboard(player.getEntity())) {
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
	public static void onVersion(CommandUser user) {
		user.sendMessage(getVersionInfo());
	}


	@Subcommand("display|d|elements|element|elem|e")
	@CommandPermission("bending.command.display")
	@CommandCompletion("@elements")
	@Description("List all available abilities for a specific element")
	public static void onDisplay(CommandUser user, Element element) {
		Collection<Component> output = new ArrayList<>(16);
		output.addAll(collectAbilities(user, element));
		output.addAll(collectSequences(user, element));
		output.addAll(collectPassives(user, element));
		if (output.isEmpty()) {
			Message.ELEMENT_ABILITIES_EMPTY.send(user, element.getDisplayName());
		} else {
			Message.ELEMENT_ABILITIES_HEADER.send(user, element.getDisplayName());
			output.forEach(user::sendMessage);
		}
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
		BendingPlayer bendingPlayer = target == null ? player : Bending.getGame().getPlayerManager().getPlayer(target.getPlayer().getUniqueId());
		Message.BOUND_SLOTS.send(player, bendingPlayer.getEntity().getName());
		for (int slot = 1; slot <= 9; slot++) {
			Component meta = bendingPlayer.getStandardSlotAbility(slot).map(AbilityDescription::getMeta).orElse(null);
			if (meta != null) player.sendMessage(Component.text(slot + ". ", NamedTextColor.DARK_AQUA).append(meta));
		}
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
	public static void onInfo(CommandUser user, AbilityDescription ability) {
		String description = ability.getDescription();
		String instructions = ability.getInstructions();
		if (description.isEmpty() && instructions.isEmpty()) {
			Message.ABILITY_INFO_EMPTY.send(user, ability.getDisplayName());
		} else {
			if (!description.isEmpty()) {
				Message.ABILITY_INFO_DESCRIPTION.send(user, ability.getDisplayName(), description);
			}
			if (!instructions.isEmpty()) {
				Message.ABILITY_INFO_INSTRUCTIONS.send(user, ability.getDisplayName(), instructions);
			}
		}

		if (ability.isActivatedBy(ActivationMethod.SEQUENCE)) {
			Bending.getGame().getSequenceManager().getSequence(ability).ifPresent(sequence -> user.sendMessage(ability.getDisplayName()
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

	private static Collection<Component> collectAbilities(CommandUser user, Element element) {
		Collection<Component> abilities = Bending.getGame().getAbilityRegistry().getAbilities()
			.filter(desc -> element == desc.getElement() && !desc.isHidden())
			.filter(desc -> !desc.isActivatedBy(ActivationMethod.SEQUENCE))
			.filter(desc -> user.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!abilities.isEmpty()) {
			Collection<Component> output = new ArrayList<>();
			output.add(Message.ABILITIES.build());
			output.addAll(abilities);
			return output;
		}
		return Collections.emptyList();
	}

	private static Collection<Component> collectSequences(CommandUser user, Element element) {
		Collection<Component> sequences = Bending.getGame().getSequenceManager().getRegisteredSequences()
			.filter(desc -> element == desc.getElement() && !desc.isHidden())
			.filter(desc -> !desc.isHidden())
			.filter(desc -> user.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!sequences.isEmpty()) {
			Collection<Component> output = new ArrayList<>();
			output.add(Message.SEQUENCES.build());
			output.addAll(sequences);
			return output;
		}
		return Collections.emptyList();
	}

	private static Collection<Component> collectPassives(CommandUser user, Element element) {
		Collection<Component> passives = Bending.getGame().getAbilityRegistry().getPassives(element)
			.filter(desc -> !desc.isHidden())
			.filter(desc -> user.hasPermission(desc.getPermission()))
			.map(AbilityDescription::getMeta)
			.collect(Collectors.toList());
		if (!passives.isEmpty()) {
			Collection<Component> output = new ArrayList<>();
			output.add(Message.PASSIVES.build());
			output.addAll(passives);
			return output;
		}
		return Collections.emptyList();
	}
}
