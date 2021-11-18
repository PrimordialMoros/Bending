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
import java.util.Locale;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.paper.PaperCommandManager;
import me.moros.bending.Bending;
import me.moros.bending.command.parser.AbilityDescriptionParser;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.description.AbilityDescription.Sequence;
import me.moros.bending.model.predicate.general.BendingConditions;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class BendingCommand {
  BendingCommand(PaperCommandManager<CommandSender> manager, MinecraftHelp<CommandSender> help) {
    Builder<CommandSender> builder = manager.commandBuilder("bending", "bend", "b", "avatar", "atla", "tla")
      .meta(CommandMeta.DESCRIPTION, "Base command for bending");

    var slotArg = IntegerArgument.<CommandSender>newBuilder("slot")
      .withMin(0).withMax(9).asOptionalWithDefault(0);
    var userArg = manager.argumentBuilder(User.class, "target")
      .asOptionalWithDefault("me");
    var abilityArg = manager.argumentBuilder(AbilityDescription.class, "ability")
      .withParser(AbilityDescriptionParser.STRICT_PARSER);

    //noinspection ConstantConditions
    manager
      .command(builder.literal("version", "ver", "v")
        .meta(CommandMeta.DESCRIPTION, "View version info about the bending plugin")
        .permission("bending.command.version")
        .handler(c -> onVersion(c.getSender()))
      ).command(builder.literal("reload", "r")
        .meta(CommandMeta.DESCRIPTION, "Reloads the plugin and its config")
        .permission("bending.command.reload")
        .handler(c -> onReload(c.getSender()))
      ).command(builder.literal("board")
        .meta(CommandMeta.DESCRIPTION, "Toggle bending board visibility")
        .permission("bending.command.board")
        .senderType(Player.class)
        .handler(c -> onBoard(c.get(ContextKeys.BENDING_PLAYER)))
      ).command(builder.literal("toggle", "t")
        .meta(CommandMeta.DESCRIPTION, "Toggles bending")
        .permission("bending.command.toggle")
        .senderType(Player.class)
        .handler(c -> onToggle(c.get(ContextKeys.BENDING_PLAYER)))
      ).command(builder.literal("info", "i")
        .meta(CommandMeta.DESCRIPTION, "View info about a specific ability")
        .permission("bending.command.help")
        .argument(manager.argumentBuilder(AbilityDescription.class, "ability"))
        .handler(c -> onInfo(c.getSender(), c.get("ability")))
      ).command(builder.literal("list", "ls", "display", "d")
        .meta(CommandMeta.DESCRIPTION, "List all available abilities for a specific element")
        .permission("bending.command.display")
        .argument(EnumArgument.of(Element.class, "element"))
        .handler(c -> ElementCommand.onElementDisplay(c.getSender(), c.get("element")))
      ).command(builder.literal("bind", "b")
        .meta(CommandMeta.DESCRIPTION, "Bind an ability to a slot")
        .permission("bending.command.bind")
        .senderType(Player.class)
        .argument(abilityArg.build())
        .argument(slotArg.build())
        .handler(c -> onBind(c.get(ContextKeys.BENDING_PLAYER), c.get("ability"), c.get("slot")))
      ).command(builder.literal("clear", "c")
        .meta(CommandMeta.DESCRIPTION, "Clear an ability slot")
        .permission("bending.command.bind")
        .senderType(Player.class)
        .argument(slotArg.build())
        .handler(c -> onClear(c.get(ContextKeys.BENDING_PLAYER), c.get("slot")))
      ).command(builder.literal("binds", "who", "w")
        .meta(CommandMeta.DESCRIPTION, "Show all bound abilities")
        .permission("bending.command.help")
        .argument(userArg.build())
        .handler(c -> onBindList(c.getSender(), c.get("target")))
      ).command(builder.literal("choose", "ch")
        .meta(CommandMeta.DESCRIPTION, "Choose an element")
        .permission("bending.command.choose")
        .senderType(Player.class)
        .argument(EnumArgument.optional(Element.class, "element"))
        .handler(c -> ElementCommand.onElementChoose(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
      ).command(builder.literal("add", "a")
        .meta(CommandMeta.DESCRIPTION, "Add an element")
        .permission("bending.command.add")
        .senderType(Player.class)
        .argument(EnumArgument.optional(Element.class, "element"))
        .handler(c -> ElementCommand.onElementAdd(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
      ).command(builder.literal("remove", "rm", "delete", "del")
        .meta(CommandMeta.DESCRIPTION, "Remove an element")
        .permission("bending.command.remove")
        .senderType(Player.class)
        .argument(EnumArgument.optional(Element.class, "element"))
        .handler(c -> ElementCommand.onElementRemove(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
      ).command(builder.literal("help", "h")
        .argument(StringArgument.optional("query", StringArgument.StringMode.GREEDY))
        .handler(c -> help.queryCommands(c.getOrDefault("query", ""), c.getSender()))
    );
  }

  private static void onVersion(CommandSender sender) {
    String link = "https://github.com/PrimordialMoros/Bending";
    Component version = Message.brand(Component.text("Version: ", NamedTextColor.DARK_AQUA))
      .append(Component.text(Bending.version(), NamedTextColor.GREEN))
      .hoverEvent(HoverEvent.showText(Message.VERSION_COMMAND_HOVER.build(Bending.author(), link)))
      .clickEvent(ClickEvent.openUrl(link));
    sender.sendMessage(version);
  }

  private static void onReload(CommandSender sender) {
    Bending.game().reload();
    Message.CONFIG_RELOAD.send(sender);
  }

  private static void onBoard(BendingPlayer player) {
    if (!Bending.game().boardManager().enabled(player.world())) {
      Message.BOARD_DISABLED.send(player);
      return;
    }
    if (!player.board()) {
      player.board(true);
      Message.BOARD_TOGGLED_ON.send(player);
    } else {
      player.board(false);
      Message.BOARD_TOGGLED_OFF.send(player);
    }
    Bending.game().boardManager().tryEnableBoard(player);
  }

  private static void onToggle(User user) {
    if (user.bendingConditional().contains(BendingConditions.TOGGLED)) {
      user.bendingConditional().remove(BendingConditions.TOGGLED);
      Message.TOGGLE_ON.send(user);
    } else {
      user.bendingConditional().add(BendingConditions.TOGGLED);
      Message.TOGGLE_OFF.send(user);
    }
  }

  private static void onInfo(CommandSender sender, AbilityDescription ability) {
    String descKey = "bending.ability." + ability.name().toLowerCase(Locale.ROOT) + ".description";
    String instKey = "bending.ability." + ability.name().toLowerCase(Locale.ROOT) + ".instructions";
    Component description = Bending.translationManager().translate(descKey);
    Component instructions = Bending.translationManager().translate(instKey);
    if (instructions == null && ability instanceof Sequence sequence) {
      instructions = sequence.instructions();
    }
    if (description == null && instructions == null) {
      Message.ABILITY_INFO_EMPTY.send(sender, ability.displayName());
    } else {
      if (description != null) {
        Message.ABILITY_INFO_DESCRIPTION.send(sender, ability.displayName(), description);
      }
      if (instructions != null) {
        Message.ABILITY_INFO_INSTRUCTIONS.send(sender, ability.displayName(), instructions);
      }
    }
  }

  private static void onBind(BendingPlayer player, AbilityDescription ability, int slot) {
    if (!player.hasElement(ability.element())) {
      Message.ABILITY_BIND_REQUIRES_ELEMENT.send(player, ability.displayName(), ability.element().displayName());
      return;
    }
    if (!ability.canBind()) {
      Message.ABILITY_BIND_FAIL.send(player, ability.displayName());
      return;
    }
    if (!player.hasPermission(ability)) {
      Message.ABILITY_BIND_NO_PERMISSION.send(player, ability.displayName());
    }
    if (slot == 0) {
      slot = player.currentSlot();
    }
    player.bindAbility(slot, ability);
    Message.ABILITY_BIND_SUCCESS.send(player, ability.displayName(), slot);
  }

  private static void onClear(BendingPlayer player, int slot) {
    if (slot == 0) {
      player.bindPreset(Preset.EMPTY);
      Message.CLEAR_ALL_SLOTS.send(player);
      return;
    }
    player.clearSlot(slot);
    Message.CLEAR_SLOT.send(player, slot);
  }

  private static void onBindList(CommandSender sender, User user) {
    Collection<Element> elements = user.elements();
    Component hover;
    if (elements.isEmpty()) {
      hover = Message.NO_ELEMENTS.build();
    } else {
      JoinConfiguration sep = JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY));
      hover = Component.join(sep, user.elements().stream().map(Element::displayName).toList());
    }
    Message.BOUND_SLOTS.send(sender, user.entity().getName(), hover);
    user.createPresetFromSlots("").display().forEach(sender::sendMessage);
  }
}
