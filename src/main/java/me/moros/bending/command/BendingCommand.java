/*
 * Copyright 2020-2021 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.command;

import java.util.Collection;
import java.util.Locale;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.minecraft.extras.MinecraftHelp.HelpColors;
import me.moros.bending.Bending;
import me.moros.bending.gui.ElementMenu;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.description.AbilityDescription.Sequence;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.predicate.general.BendingConditions;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.ChatUtil;
import me.moros.bending.util.ColorPalette;
import me.moros.bending.util.packet.PacketUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class BendingCommand {
  private final CommandManager manager;
  private final Builder<CommandSender> builder;
  private final MinecraftHelp<CommandSender> help;

  BendingCommand(@NonNull CommandManager manager) {
    this.manager = manager;
    this.builder = manager.rootBuilder();
    this.help = MinecraftHelp.createNative("/bending help", manager);
    help.setMaxResultsPerPage(9);
    help.setHelpColors(HelpColors.of(
      ColorPalette.NEUTRAL,
      ColorPalette.TEXT_COLOR,
      ColorPalette.ACCENT,
      ColorPalette.HEADER,
      ColorPalette.NEUTRAL)
    );
    construct();
  }

  private void construct() {
    Builder<CommandSender> presetBase = builder.literal("preset", "presets", "pre", "p")
      .permission(CommandPermissions.PRESET);
    Builder<CommandSender> modifierBase = builder.literal("modifier", "modifiers", "mod", "m")
      .permission(CommandPermissions.MODIFY);

    var slotArg = IntegerArgument.<CommandSender>newBuilder("slot")
      .withMin(0).withMax(9).asOptionalWithDefault(0);
    var userArg = manager.argumentBuilder(User.class, "target")
      .asOptionalWithDefault("me");
    var abilityArg = manager.argumentBuilder(AbilityDescription.class, "ability");
    var helpArg = StringArgument.<CommandSender>newBuilder("query")
      .greedy().withSuggestionsProvider((c, s) -> CommandManager.combinedSuggestions(c.getSender())).asOptional();

    //noinspection ConstantConditions
    manager
      .command(builder.handler(c -> onHelp(help, "", c.getSender())))
      .command(builder.literal("version", "v")
        .meta(CommandMeta.DESCRIPTION, "View version info about the bending plugin")
        .permission(CommandPermissions.VERSION)
        .handler(c -> onVersion(c.getSender()))
      ).command(builder.literal("reload")
        .meta(CommandMeta.DESCRIPTION, "Reloads the plugin and its config")
        .permission(CommandPermissions.RELOAD)
        .handler(c -> onReload(c.getSender()))
      ).command(builder.literal("board")
        .meta(CommandMeta.DESCRIPTION, "Toggle bending board visibility")
        .permission(CommandPermissions.BOARD)
        .senderType(Player.class)
        .handler(c -> onBoard(c.get(ContextKeys.BENDING_PLAYER)))
      ).command(builder.literal("toggle", "t")
        .meta(CommandMeta.DESCRIPTION, "Toggles bending")
        .permission(CommandPermissions.TOGGLE)
        .senderType(Player.class)
        .handler(c -> onToggle(c.get(ContextKeys.BENDING_PLAYER)))
      ).command(builder.literal("bind", "b")
        .meta(CommandMeta.DESCRIPTION, "Bind an ability to a slot")
        .permission(CommandPermissions.BIND)
        .senderType(Player.class)
        .argument(abilityArg.build())
        .argument(slotArg.build())
        .handler(c -> onBind(c.get(ContextKeys.BENDING_PLAYER), c.get("ability"), c.get("slot")))
      ).command(builder.literal("clear", "c")
        .meta(CommandMeta.DESCRIPTION, "Clear an ability slot")
        .permission(CommandPermissions.BIND)
        .senderType(Player.class)
        .argument(slotArg.build())
        .handler(c -> onClear(c.get(ContextKeys.BENDING_PLAYER), c.get("slot")))
      ).command(builder.literal("who", "w")
        .meta(CommandMeta.DESCRIPTION, "Show all bound abilities")
        .permission(CommandPermissions.HELP)
        .argument(userArg.build())
        .handler(c -> onBindList(c.getSender(), c.get("target")))
      ).command(builder.literal("choose", "ch")
        .meta(CommandMeta.DESCRIPTION, "Choose an element")
        .permission(CommandPermissions.CHOOSE)
        .senderType(Player.class)
        .argument(EnumArgument.optional(Element.class, "element"))
        .handler(c -> onElementChooseOptional(c.get(ContextKeys.BENDING_PLAYER), c.getOrDefault("element", null)))
      ).command(builder.literal("add", "a")
        .meta(CommandMeta.DESCRIPTION, "Add an element")
        .permission(CommandPermissions.ADD)
        .senderType(Player.class)
        .argument(EnumArgument.of(Element.class, "element"))
        .handler(c -> onElementAdd(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
      ).command(builder.literal("remove", "rm")
        .meta(CommandMeta.DESCRIPTION, "Remove an element")
        .permission(CommandPermissions.REMOVE)
        .senderType(Player.class)
        .argument(EnumArgument.of(Element.class, "element"))
        .handler(c -> onElementRemove(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
      ).command(builder.literal("help", "h")
        .meta(CommandMeta.DESCRIPTION, "View info about an element, ability or command")
        .permission(CommandPermissions.HELP)
        .argument(helpArg.build())
        .handler(c -> onHelp(help, c.getOrDefault("query", ""), c.getSender()))
      ).command(presetBase.literal("list", "ls")
        .meta(CommandMeta.DESCRIPTION, "List all available presets")
        .senderType(Player.class)
        .handler(c -> onList(c.get(ContextKeys.BENDING_PLAYER)))
      ).command(presetBase.literal("create", "save", "c")
        .meta(CommandMeta.DESCRIPTION, "Create a new preset")
        .senderType(Player.class)
        .argument(StringArgument.single("name"))
        .handler(c -> onCreate(c.get(ContextKeys.BENDING_PLAYER), c.get("name")))
      ).command(presetBase.literal("remove", "rm")
        .meta(CommandMeta.DESCRIPTION, "Remove an existing preset")
        .senderType(Player.class)
        .argument(manager.argumentBuilder(Preset.class, "preset"))
        .handler(c -> onRemove(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
      ).command(presetBase.literal("bind", "b")
        .meta(CommandMeta.DESCRIPTION, "Bind an existing preset")
        .senderType(Player.class)
        .argument(manager.argumentBuilder(Preset.class, "preset"))
        .handler(c -> onBind(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
      ).command(modifierBase.literal("add", "a")
        .meta(CommandMeta.DESCRIPTION, "Add a new modifier to the specified user")
        .senderType(Player.class)
        .argument(manager.argumentBuilder(ModifyPolicy.class, "policy"))
        .argument(EnumArgument.of(Attribute.class, "attribute"))
        .argument(EnumArgument.of(ModifierOperation.class, "operation"))
        .argument(DoubleArgument.of("amount"))
        .handler(c -> {
          AttributeModifier modifier = new AttributeModifier(c.get("policy"), c.get("attribute"), c.get("operation"), c.get("amount"));
          onModifierAdd(c.get(ContextKeys.BENDING_PLAYER), modifier);
        })
      ).command(modifierBase.literal("clear", "c")
        .meta(CommandMeta.DESCRIPTION, "Clear all existing modifiers for a user")
        .senderType(Player.class)
        .handler(c -> onModifierClear(c.get(ContextKeys.BENDING_PLAYER)))
      );
  }

  private static void onHelp(MinecraftHelp<CommandSender> help, String rawQuery, CommandSender sender) {
    if (!rawQuery.isEmpty()) {
      int index = rawQuery.indexOf(' ');
      String query = rawQuery.substring(0, index > 0 ? index : rawQuery.length());
      if (query.length() <= 5) {
        Element element = Element.fromName(query).orElse(null);
        if (element != null) {
          onElementDisplay(sender, element);
          return;
        }
      } else {
        AbilityDescription result = Registries.ABILITIES.ability(query);
        if (result != null && !result.hidden() && sender.hasPermission(result.permission())) {
          onInfo(sender, result);
          return;
        }
      }
    }
    help.queryCommands(rawQuery, sender);
  }

  private static void onVersion(CommandSender sender) {
    String link = "https://github.com/PrimordialMoros/Bending";
    Component version = Component.text("Bending " + Bending.version(), ColorPalette.HEADER)
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

  private static void onElementChooseOptional(BendingPlayer player, Element element) {
    if (element == null) {
      new ElementMenu(player);
    } else {
      onElementChoose(player, element);
    }
  }

  private static void sendElementNotification(User user, Element element) {
    if (user instanceof BendingPlayer player) {
      Component title = Component.text().append(Component.text("You can now bend", ColorPalette.TEXT_COLOR))
        .append(Component.space()).append(element.displayName()).build();
      PacketUtil.sendNotification(player.entity(), Material.NETHER_STAR, title);
    }
  }

  public static void onElementChoose(@NonNull User user, @NonNull Element element) {
    if (!user.hasPermission("bending.command.choose." + element)) {
      Message.ELEMENT_CHOOSE_NO_PERMISSION.send(user, element.displayName());
      return;
    }
    if (user.chooseElement(element)) {
      Message.ELEMENT_CHOOSE_SUCCESS.send(user, element.displayName());
      sendElementNotification(user, element);
    } else {
      Message.ELEMENT_CHOOSE_FAIL.send(user, element.displayName());
    }
  }

  public static void onElementAdd(@NonNull User user, @NonNull Element element) {
    if (!user.hasPermission("bending.command.add." + element)) {
      Message.ELEMENT_ADD_NO_PERMISSION.send(user, element.displayName());
      return;
    }
    if (user.addElement(element)) {
      Bending.game().abilityManager(user.world()).createPassives(user);
      Message.ELEMENT_ADD_SUCCESS.send(user, element.displayName());
      sendElementNotification(user, element);
    } else {
      Message.ELEMENT_ADD_FAIL.send(user, element.displayName());
    }
  }

  public static void onElementRemove(@NonNull User user, @NonNull Element element) {
    if (user.removeElement(element)) {
      Bending.game().abilityManager(user.world()).createPassives(user);
      Message.ELEMENT_REMOVE_SUCCESS.send(user, element.displayName());
    } else {
      Message.ELEMENT_REMOVE_FAIL.send(user, element.displayName());
    }
  }

  public static void onElementDisplay(@NonNull User user, @NonNull Element element) {
    onElementDisplay(user.entity(), element);
  }

  public static void onElementDisplay(@NonNull CommandSender user, @NonNull Element element) {
    Collection<Component> abilities = collectAbilities(user, element);
    Collection<Component> sequences = collectSequences(user, element);
    Collection<Component> passives = collectPassives(user, element);
    if (abilities.isEmpty() && sequences.isEmpty() && passives.isEmpty()) {
      Message.ELEMENT_ABILITIES_EMPTY.send(user, element.displayName());
    } else {
      Message.ELEMENT_ABILITIES_HEADER.send(user, element.displayName(), element.description());
      JoinConfiguration sep = JoinConfiguration.separator(Component.text(", ", ColorPalette.TEXT_COLOR));
      if (!abilities.isEmpty()) {
        Message.ABILITIES.send(user);
        user.sendMessage(Component.join(sep, abilities));
      }
      if (!sequences.isEmpty()) {
        Message.SEQUENCES.send(user);
        user.sendMessage(Component.join(sep, sequences));
      }
      if (!passives.isEmpty()) {
        Message.PASSIVES.send(user);
        user.sendMessage(Component.join(sep, passives));
      }
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
    if (!ability.canBind()) {
      Message.ABILITY_BIND_FAIL.send(player, ability.displayName());
      return;
    }
    if (!player.hasPermission(ability)) {
      Message.ABILITY_BIND_NO_PERMISSION.send(player, ability.displayName());
    }
    if (!player.hasElement(ability.element())) {
      Message.ABILITY_BIND_REQUIRES_ELEMENT.send(player, ability.displayName(), ability.element().displayName());
      return;
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
      JoinConfiguration sep = JoinConfiguration.separator(Component.text(", ", ColorPalette.TEXT_COLOR));
      hover = Component.join(sep, user.elements().stream().map(Element::displayName).toList());
    }
    Message.BOUND_SLOTS.send(sender, user.entity().getName(), hover);
    user.createPresetFromSlots("").display().forEach(sender::sendMessage);
  }

  private static void onList(BendingPlayer player) {
    Collection<Preset> presets = player.presets();
    if (presets.isEmpty()) {
      Message.NO_PRESETS.send(player);
    } else {
      Message.PRESET_LIST_HEADER.send(player);
      JoinConfiguration sep = JoinConfiguration.separator(Component.text(", ", ColorPalette.TEXT_COLOR));
      player.sendMessage(Component.join(sep, presets.stream().map(Preset::meta).toList()));
    }
  }

  private static void onCreate(BendingPlayer player, String name) {
    String input = ChatUtil.sanitizeInput(name);
    if (input.isEmpty()) {
      Message.INVALID_PRESET_NAME.send(player);
      return;
    }
    Preset preset = player.createPresetFromSlots(input);
    if (preset.isEmpty()) {
      Message.EMPTY_PRESET.send(player);
      return;
    }
    player.addPreset(preset).thenAccept(result -> result.message().send(player, input));
  }

  private static void onRemove(BendingPlayer player, Preset preset) {
    if (player.removePreset(preset)) {
      Message.PRESET_REMOVE_SUCCESS.send(player, preset.name());
    } else {
      Message.PRESET_REMOVE_FAIL.send(player, preset.name());
    }
  }

  private static void onBind(BendingPlayer player, Preset preset) {
    if (player.bindPreset(preset)) {
      Message.PRESET_BIND_SUCCESS.send(player, preset.name());
    } else {
      Message.PRESET_BIND_FAIL.send(player, preset.name());
    }
  }

  private static void onModifierAdd(User user, AttributeModifier modifier) {
    Registries.ATTRIBUTES.add(user, modifier);
    recalculate(user);
    Message.MODIFIER_ADD.send(user, user.entity().getName());
  }

  private static void onModifierClear(User user) {
    Registries.ATTRIBUTES.invalidate(user);
    recalculate(user);
    Message.MODIFIER_CLEAR.send(user, user.entity().getName());
  }

  private static void recalculate(User user) {
    Bending.game().abilityManager(user.world()).userInstances(user).forEach(Ability::loadConfig);
  }

  private static Collection<Component> collectAbilities(CommandSender user, Element element) {
    return Registries.ABILITIES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(desc -> !desc.isActivatedBy(Activation.SEQUENCE) && !desc.isActivatedBy(Activation.PASSIVE))
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .toList();
  }

  private static Collection<Component> collectSequences(CommandSender user, Element element) {
    return Registries.SEQUENCES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .toList();
  }

  private static Collection<Component> collectPassives(CommandSender user, Element element) {
    return Registries.ABILITIES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden() && desc.isActivatedBy(Activation.PASSIVE))
      .filter(desc -> user.hasPermission(desc.permission()))
      .map(AbilityDescription::meta)
      .toList();
  }
}
