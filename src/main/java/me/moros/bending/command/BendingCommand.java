/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
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

import java.util.Collection;
import java.util.stream.Collectors;

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
import me.moros.bending.command.Commands.UserException;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.sequence.Sequence;
import me.moros.bending.model.predicate.general.BendingConditions;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("%bendingcommand")
public class BendingCommand extends BaseCommand {
  @HelpCommand
  @CommandPermission("bending.command.help")
  public static void doHelp(CommandSender user, CommandHelp help) {
    Message.HELP_HEADER.send(user);
    help.showHelp();
  }

  @Subcommand("toggle|t")
  @CommandCompletion("@players")
  @CommandPermission("bending.command.toggle")
  @Description("Toggles bending")
  public void onToggle(BendingPlayer player, @Optional @CommandPermission("bending.command.toggle.others") OnlinePlayer target) {
    BendingPlayer bendingPlayer = target == null ? player : Bending.game().benderRegistry().player(target.getPlayer());
    if (bendingPlayer.bendingConditional().contains(BendingConditions.TOGGLED)) {
      bendingPlayer.bendingConditional().remove(BendingConditions.TOGGLED);
      Message.TOGGLE_ON.send(bendingPlayer);
    } else {
      bendingPlayer.bendingConditional().add(BendingConditions.TOGGLED);
      Message.TOGGLE_OFF.send(bendingPlayer);
    }
  }

  @Subcommand("reload")
  @CommandPermission("bending.command.reload")
  @Description("Reloads the plugin and its config")
  public void onReload(CommandSender user) {
    Bending.game().reload();
    Message.CONFIG_RELOAD.send(user);
  }

  @Subcommand("choose|ch")
  @CommandPermission("bending.command.choose")
  @CommandCompletion("@elements @players")
  @Description("Choose an element")
  public static void onElementChoose(CommandSender user, Element element, @Optional @CommandPermission("bending.command.choose.other") OnlinePlayer target) {
    if (target == null && !(user instanceof Player)) {
      throw new UserException();
    }
    BendingPlayer player = Bending.game().benderRegistry().player(target == null ? (Player) user : target.getPlayer());
    if (target == null && !player.hasPermission("bending.command.choose." + element)) {
      Message.ELEMENT_CHOOSE_NO_PERMISSION.send(player, element.displayName());
      return;
    }
    if (player.chooseElement(element)) {
      Message.ELEMENT_CHOOSE_SUCCESS.send(player, element.displayName());
    } else {
      Message.ELEMENT_CHOOSE_FAIL.send(player, element.displayName());
    }
  }

  @Subcommand("add|a")
  @CommandPermission("bending.command.add")
  @CommandCompletion("@elements @players")
  @Description("Add an element")
  public static void onElementAdd(CommandSender user, Element element, @Optional @CommandPermission("bending.command.add.other") OnlinePlayer target) {
    if (target == null && !(user instanceof Player)) {
      throw new UserException();
    }
    BendingPlayer player = Bending.game().benderRegistry().player(target == null ? (Player) user : target.getPlayer());
    if (target == null && !player.hasPermission("bending.command.add." + element)) {
      Message.ELEMENT_ADD_NO_PERMISSION.send(player, element.displayName());
      return;
    }
    if (player.addElement(element)) {
      Bending.game().abilityManager(player.world()).createPassives(player);
      Message.ELEMENT_ADD_SUCCESS.send(player, element.displayName());
    } else {
      Message.ELEMENT_ADD_FAIL.send(player, element.displayName());
    }
  }

  @Subcommand("remove|rm|r|delete|del")
  @CommandPermission("bending.command.remove")
  @CommandCompletion("@elements @players")
  @Description("Remove an element")
  public static void onElementRemove(CommandSender user, Element element, @Optional @CommandPermission("bending.command.remove.other") OnlinePlayer target) {
    if (target == null && !(user instanceof Player)) {
      throw new UserException();
    }
    BendingPlayer player = Bending.game().benderRegistry().player(target == null ? (Player) user : target.getPlayer());
    if (player.removeElement(element)) {
      Bending.game().abilityManager(player.world()).createPassives(player);
      Message.ELEMENT_REMOVE_SUCCESS.send(player, element.displayName());
    } else {
      Message.ELEMENT_REMOVE_FAIL.send(player, element.displayName());
    }
  }

  @Subcommand("bendingboard|board|bb")
  @CommandPermission("bending.command.board")
  @Description("Toggle bending board visibility")
  public static void onBoard(BendingPlayer player) {
    if (Bending.game().isDisabledWorld(player.world().getUID())) {
      Message.BOARD_DISABLED.send(player);
      return;
    }
    if (Bending.game().boardManager().toggleScoreboard(player.entity())) {
      player.profile().board(true);
      Message.BOARD_TOGGLED_ON.send(player);
    } else {
      player.profile().board(false);
      Message.BOARD_TOGGLED_OFF.send(player);
    }
  }

  @Subcommand("version|ver|v")
  @CommandPermission("bending.command.help")
  @Description("View version info about the bending plugin")
  public static void onVersion(CommandSender user) {
    String link = "https://github.com/PrimordialMoros/Bending";
    Component version = Message.brand(Component.text("Version: ", NamedTextColor.DARK_AQUA))
      .append(Component.text(Bending.version(), NamedTextColor.GREEN))
      .hoverEvent(HoverEvent.showText(Message.VERSION_COMMAND_HOVER.build(Bending.author(), link)))
      .clickEvent(ClickEvent.openUrl(link));
    user.sendMessage(version);
  }

  @Subcommand("display|d|elements|element|elem|e")
  @CommandPermission("bending.command.display")
  @CommandCompletion("@elements")
  @Description("List all available abilities for a specific element")
  public static void onDisplay(CommandSender user, Element element) {
    Collection<Component> abilities = collectAbilities(user, element);
    Collection<Component> sequences = collectSequences(user, element);
    Collection<Component> passives = collectPassives(user, element);
    if (abilities.isEmpty() && sequences.isEmpty() && passives.isEmpty()) {
      Message.ELEMENT_ABILITIES_EMPTY.send(user, element.displayName());
    } else {
      Message.ELEMENT_ABILITIES_HEADER.send(user, element.displayName());
      if (!abilities.isEmpty()) {
        Message.ABILITIES.send(user);
        user.sendMessage(Component.join(Component.text(", ", NamedTextColor.WHITE), abilities));
      }
      if (!sequences.isEmpty()) {
        Message.SEQUENCES.send(user);
        user.sendMessage(Component.join(Component.text(", ", NamedTextColor.WHITE), sequences));
      }
      if (!passives.isEmpty()) {
        Message.PASSIVES.send(user);
        user.sendMessage(Component.join(Component.text(", ", NamedTextColor.WHITE), passives));
      }
    }
  }

  @Subcommand("bind|b")
  @CommandPermission("bending.command.bind")
  @CommandCompletion("@abilities @range:1-9")
  @Description("Bind an ability to a slot")
  public static void onBind(BendingPlayer player, AbilityDescription ability, @Default("0") @Conditions("slot") Integer slot) {
    if (!player.hasElement(ability.element())) {
      Message.ABILITY_BIND_REQUIRES_ELEMENT.send(player, ability.displayName(), ability.element().displayName());
      return;
    }
    if (!ability.canBind()) {
      Message.ABILITY_BIND_FAIL.send(player, ability.displayName());
      return;
    }
    if (slot == 0) {
      slot = player.currentSlot();
    }
    player.bindAbility(slot, ability);
    Message.ABILITY_BIND_SUCCESS.send(player, ability.displayName(), slot);
  }

  @Subcommand("list|ls|binds")
  @CommandPermission("bending.command.help")
  @CommandCompletion("@players")
  @Description("Show all bound abilities")
  public static void onBindList(BendingPlayer player, @Optional OnlinePlayer target) {
    BendingPlayer bendingPlayer = target == null ? player : Bending.game().benderRegistry().player(target.getPlayer());
    Message.BOUND_SLOTS.send(player, bendingPlayer.entity().getName());
    for (int slot = 1; slot <= 9; slot++) {
      Component meta = bendingPlayer.boundAbility(slot).map(AbilityDescription::meta).orElse(null);
      if (meta != null) {
        player.sendMessage(Component.text(slot + ". ", NamedTextColor.DARK_AQUA).append(meta));
      }
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
    player.clearSlot(slot);
    Message.CLEAR_SLOT.send(player, slot);
  }

  @Subcommand("info|i")
  @CommandPermission("bending.command.help")
  @CommandCompletion("@allabilities")
  @Description("View info about a specific ability")
  public static void onInfo(CommandSender user, AbilityDescription ability) {
    String descKey = "bending.ability." + ability.name().toLowerCase() + ".description";
    String instKey = "bending.ability." + ability.name().toLowerCase() + ".instructions";
    Component description = Bending.translationManager().getTranslation(descKey);
    Component instructions = Bending.translationManager().getTranslation(instKey);
    if (instructions == null && ability.isActivatedBy(ActivationMethod.SEQUENCE)) {
      Sequence sequence = Bending.game().sequenceManager().sequence(ability);
      if (sequence != null) {
        instructions = sequence.instructions();
      }
    }
    if (description == null && instructions == null) {
      Message.ABILITY_INFO_EMPTY.send(user, ability.displayName());
    } else {
      if (description != null) {
        Message.ABILITY_INFO_DESCRIPTION.send(user, ability.displayName(), description);
      }
      if (instructions != null) {
        Message.ABILITY_INFO_INSTRUCTIONS.send(user, ability.displayName(), instructions);
      }
    }
  }

  private static Collection<Component> collectAbilities(CommandSender user, Element element) {
    return Bending.game().abilityRegistry().abilities()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(desc -> !desc.isActivatedBy(ActivationMethod.SEQUENCE))
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .collect(Collectors.toList());
  }

  private static Collection<Component> collectSequences(CommandSender user, Element element) {
    return Bending.game().sequenceManager().sequences()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(desc -> !desc.hidden())
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .collect(Collectors.toList());
  }

  private static Collection<Component> collectPassives(CommandSender user, Element element) {
    return Bending.game().abilityRegistry().passives(element)
      .filter(desc -> !desc.hidden())
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .collect(Collectors.toList());
  }
}
