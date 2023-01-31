/*
 * Copyright 2020-2023 Moros
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
import java.util.Iterator;
import java.util.List;

import cloud.commandframework.Command.Builder;
import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.standard.DoubleArgument;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.minecraft.extras.MinecraftHelp;
import cloud.commandframework.minecraft.extras.MinecraftHelp.HelpColors;
import me.moros.bending.BendingPlugin;
import me.moros.bending.locale.Message;
import me.moros.bending.model.Element;
import me.moros.bending.model.ElementHandler;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityDescription.Sequence;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.board.Board;
import me.moros.bending.model.manager.Game;
import me.moros.bending.model.preset.Preset;
import me.moros.bending.model.registry.Registries;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.Platform;
import me.moros.bending.platform.entity.player.Player;
import me.moros.bending.platform.item.Item;
import me.moros.bending.util.ColorPalette;
import me.moros.bending.util.TextUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BendingCommand<T extends Audience> implements ElementHandler {
  private final BendingPlugin plugin;
  private final Game game;
  private final Class<? extends T> playerClass;
  private final CommandManager<T> manager;
  private final MinecraftHelp<T> help;

  BendingCommand(BendingPlugin plugin, Game game, CommandManager<T> manager, Class<? extends T> playerClass) {
    this.plugin = plugin;
    this.game = game;
    this.manager = manager;
    this.playerClass = playerClass;
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
    Builder<T> builder = manager.commandBuilder("bending", "bend", "b", "avatar", "atla", "tla")
      .meta(CommandMeta.DESCRIPTION, "Base command for bending");
    Builder<T> presetBase = builder.literal("preset", "presets", "pre", "p")
      .permission(CommandPermissions.PRESET);
    Builder<T> modifierBase = builder.literal("modifier", "modifiers", "mod", "m")
      .permission(CommandPermissions.MODIFY);

    var slotArg = IntegerArgument.<T>builder("slot")
      .withMin(0).withMax(9).asOptionalWithDefault(0);
    var userArg = manager.argumentBuilder(User.class, "target")
      .asOptionalWithDefault("me");
    var abilityArg = manager.argumentBuilder(AbilityDescription.class, "ability");
    var helpArg = StringArgument.<T>builder("query")
      .greedy().withSuggestionsProvider((c, s) -> BendingCommandManager.combinedSuggestions(c.getSender())).asOptional();

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
        .senderType(playerClass)
        .handler(c -> onBoard(c.get(ContextKeys.BENDING_PLAYER)))
      ).command(builder.literal("toggle", "t")
        .meta(CommandMeta.DESCRIPTION, "Toggles bending")
        .permission(CommandPermissions.TOGGLE)
        .senderType(playerClass)
        .handler(c -> onToggle(c.get(ContextKeys.BENDING_PLAYER)))
      ).command(builder.literal("bind", "b")
        .meta(CommandMeta.DESCRIPTION, "Bind an ability to a slot")
        .permission(CommandPermissions.BIND)
        .senderType(playerClass)
        .argument(abilityArg.build())
        .argument(slotArg.build())
        .handler(c -> onBind(c.get(ContextKeys.BENDING_PLAYER), c.get("ability"), c.get("slot")))
      ).command(builder.literal("clear", "c")
        .meta(CommandMeta.DESCRIPTION, "Clear an ability slot")
        .permission(CommandPermissions.BIND)
        .senderType(playerClass)
        .argument(slotArg.build())
        .handler(c -> onBindClear(c.get(ContextKeys.BENDING_PLAYER), c.get("slot")))
      ).command(builder.literal("who", "w")
        .meta(CommandMeta.DESCRIPTION, "Show all bound abilities")
        .permission(CommandPermissions.HELP)
        .argument(userArg.build())
        .handler(c -> onBindList(c.getSender(), c.get("target")))
      ).command(builder.literal("choose", "ch")
        .meta(CommandMeta.DESCRIPTION, "Choose an element")
        .permission(CommandPermissions.CHOOSE)
        .senderType(playerClass)
        .argument(EnumArgument.optional(Element.class, "element"))
        .handler(c -> onElementChooseOptional(c.get(ContextKeys.BENDING_PLAYER), c.getOrDefault("element", null)))
      ).command(builder.literal("add", "a")
        .meta(CommandMeta.DESCRIPTION, "Add an element")
        .permission(CommandPermissions.ADD)
        .senderType(playerClass)
        .argument(EnumArgument.of(Element.class, "element"))
        .handler(c -> onElementAdd(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
      ).command(builder.literal("remove", "rm")
        .meta(CommandMeta.DESCRIPTION, "Remove an element")
        .permission(CommandPermissions.REMOVE)
        .senderType(playerClass)
        .argument(EnumArgument.of(Element.class, "element"))
        .handler(c -> onElementRemove(c.get(ContextKeys.BENDING_PLAYER), c.get("element")))
      ).command(builder.literal("help", "h")
        .meta(CommandMeta.DESCRIPTION, "View info about an element, ability or command")
        .permission(CommandPermissions.HELP)
        .argument(helpArg.build())
        .handler(c -> onHelp(help, c.getOrDefault("query", ""), c.getSender()))
      ).command(presetBase.literal("list", "ls")
        .meta(CommandMeta.DESCRIPTION, "List all available presets")
        .senderType(playerClass)
        .handler(c -> onPresetList(c.get(ContextKeys.BENDING_PLAYER)))
      ).command(presetBase.literal("create", "save", "c")
        .meta(CommandMeta.DESCRIPTION, "Create a new preset")
        .senderType(playerClass)
        .argument(StringArgument.single("name"))
        .handler(c -> onPresetCreate(c.get(ContextKeys.BENDING_PLAYER), c.get("name")))
      ).command(presetBase.literal("remove", "rm")
        .meta(CommandMeta.DESCRIPTION, "Remove an existing preset")
        .senderType(playerClass)
        .argument(manager.argumentBuilder(Preset.class, "preset"))
        .handler(c -> onPresetRemove(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
      ).command(presetBase.literal("bind", "b")
        .meta(CommandMeta.DESCRIPTION, "Bind an existing preset")
        .senderType(playerClass)
        .argument(manager.argumentBuilder(Preset.class, "preset"))
        .handler(c -> onPresetBind(c.get(ContextKeys.BENDING_PLAYER), c.get("preset")))
      ).command(modifierBase.literal("add", "a")
        .meta(CommandMeta.DESCRIPTION, "Add a new modifier to the specified user")
        .senderType(playerClass)
        .argument(manager.argumentBuilder(ModifyPolicy.class, "policy"))
        .argument(EnumArgument.of(Attribute.class, "attribute"))
        .argument(EnumArgument.of(ModifierOperation.class, "operation"))
        .argument(DoubleArgument.of("amount"))
        .argument(userArg.build())
        .handler(c -> {
          AttributeModifier modifier = new AttributeModifier(c.get("policy"), c.get("attribute"), c.get("operation"), c.get("amount"));
          onModifierAdd(c.getSender(), modifier, c.get("target"));
        })
      ).command(modifierBase.literal("clear", "c")
        .meta(CommandMeta.DESCRIPTION, "Clear all existing modifiers for a user")
        .senderType(playerClass)
        .argument(userArg.build())
        .handler(c -> onModifierClear(c.getSender(), c.get("target")))
      );
  }

  private void onHelp(MinecraftHelp<T> help, String rawQuery, T sender) {
    if (!rawQuery.isEmpty()) {
      int index = rawQuery.indexOf(' ');
      String query = rawQuery.substring(0, index > 0 ? index : rawQuery.length());
      if (query.length() <= 5) {
        Element element = Element.fromName(query);
        if (element != null) {
          onElementDisplay(sender, element);
          return;
        }
      } else {
        AbilityDescription result = Registries.ABILITIES.fromString(query);
        if (result != null && !result.hidden() && hasPermission(sender, result)) {
          onInfo(sender, result);
          return;
        }
      }
    }
    help.queryCommands(rawQuery, sender);
  }

  private void onVersion(T sender) {
    String link = "https://github.com/PrimordialMoros/Bending";
    Component version = Component.text("Bending " + plugin.version(), ColorPalette.HEADER)
      .hoverEvent(HoverEvent.showText(Message.VERSION_COMMAND_HOVER.build(plugin.author(), link)))
      .clickEvent(ClickEvent.openUrl(link));
    sender.sendMessage(version);
  }

  private void onReload(T sender) {
    game.reload();
    Message.RELOAD.send(sender);
  }

  private void onBoard(BendingPlayer player) {
    boolean hidden = player.store().has(Board.HIDDEN);
    if (!player.board().isEnabled() && !hidden) {
      Message.BOARD_DISABLED.send(player);
      return;
    }
    if (hidden) {
      player.store().remove(Board.HIDDEN);
      Message.BOARD_TOGGLED_ON.send(player);
    } else {
      player.store().add(Board.HIDDEN, Board.dummy());
      Message.BOARD_TOGGLED_OFF.send(player);
    }
    player.board();
  }

  private void onToggle(User user) {
    if (user.toggleBending()) {
      Message.TOGGLE_ON.send(user);
    } else {
      Message.TOGGLE_OFF.send(user);
    }
  }

  private void onElementChooseOptional(BendingPlayer player, @Nullable Element element) {
    if (element == null) {
      if (!player.hasPermission(CommandPermissions.HELP.toString())) {
        Message.GUI_NO_PERMISSION.send(player);
        return;
      }
      Platform.instance().factory().buildMenu(this, player);
    } else {
      onElementChoose(player, element);
    }
  }

  private void sendElementNotification(User user, Element element) {
    if (user.entity() instanceof Player player) {
      player.sendNotification(Item.NETHER_STAR, Message.ELEMENT_TOAST_NOTIFICATION.build(element.displayName()));
    }
  }

  @Override
  public void onElementChoose(User user, Element element) {
    if (!user.hasPermission(CommandPermissions.CHOOSE + "." + element.key().value())) {
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

  @Override
  public void onElementAdd(User user, Element element) {
    if (user.addElement(element)) {
      Message.ELEMENT_ADD_SUCCESS.send(user, element.displayName());
      sendElementNotification(user, element);
    } else {
      Message.ELEMENT_ADD_FAIL.send(user, element.displayName());
    }
  }

  @Override
  public void onElementRemove(User user, Element element) {
    if (user.removeElement(element)) {
      Message.ELEMENT_REMOVE_SUCCESS.send(user, element.displayName());
    } else {
      Message.ELEMENT_REMOVE_FAIL.send(user, element.displayName());
    }
  }

  @Override
  public void onElementDisplay(Audience user, Element element) {
    AbilityDisplay abilities = collectAbilities(user, element);
    AbilityDisplay sequences = collectSequences(user, element);
    AbilityDisplay passives = collectPassives(user, element);
    if (abilities.isEmpty() && sequences.isEmpty() && passives.isEmpty()) {
      Message.ELEMENT_ABILITIES_EMPTY.send(user, element.displayName());
    } else {
      Message.ELEMENT_ABILITIES_HEADER.send(user, element.displayName(), element.description());
      abilities.forEach(user::sendMessage);
      sequences.forEach(user::sendMessage);
      passives.forEach(user::sendMessage);
    }
  }

  private void onInfo(T sender, AbilityDescription ability) {
    Component description = plugin.translationManager().translate(ability.translationKey() + ".description");
    Component instructions = plugin.translationManager().translate(ability.translationKey() + ".instructions");
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

  private void onBind(BendingPlayer player, AbilityDescription ability, int slot) {
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

  private void onBindClear(User user, int slot) {
    if (slot == 0) {
      user.bindPreset(Preset.EMPTY);
      Message.CLEAR_ALL_SLOTS.send(user);
      return;
    }
    user.clearSlot(slot);
    Message.CLEAR_SLOT.send(user, slot);
  }

  private void onBindList(T sender, User user) {
    Collection<Element> elements = user.elements();
    Component hover;
    if (elements.isEmpty()) {
      hover = Message.NO_ELEMENTS.build();
    } else {
      JoinConfiguration sep = JoinConfiguration.commas(true);
      hover = Component.join(sep, elements.stream().map(Element::displayName).toList())
        .colorIfAbsent(ColorPalette.TEXT_COLOR);
    }
    Message.BOUND_SLOTS.send(sender, user.name().hoverEvent(HoverEvent.showText(hover)));
    user.createPresetFromSlots("").display().forEach(sender::sendMessage);
  }

  private void onPresetList(BendingPlayer player) {
    Collection<Preset> presets = player.presets();
    if (presets.isEmpty()) {
      Message.NO_PRESETS.send(player);
    } else {
      Message.PRESET_LIST_HEADER.send(player);
      JoinConfiguration sep = JoinConfiguration.commas(true);
      player.sendMessage(Component.join(sep, presets.stream().map(Preset::meta).toList()).colorIfAbsent(ColorPalette.TEXT_COLOR));
    }
  }

  private void onPresetCreate(BendingPlayer player, String name) {
    String input = TextUtil.sanitizeInput(name);
    if (input.isEmpty()) {
      Message.INVALID_PRESET_NAME.send(player);
      return;
    }
    Preset preset = player.createPresetFromSlots(input);
    if (preset.isEmpty()) {
      Message.EMPTY_PRESET.send(player);
      return;
    }
    player.addPreset(preset).thenAccept(result -> result.send(player, input));
  }

  private void onPresetRemove(BendingPlayer player, Preset preset) {
    if (player.removePreset(preset)) {
      Message.PRESET_REMOVE_SUCCESS.send(player, preset.name());
    } else {
      Message.PRESET_REMOVE_FAIL.send(player, preset.name());
    }
  }

  private void onPresetBind(User user, Preset preset) {
    if (user.bindPreset(preset)) {
      Message.PRESET_BIND_SUCCESS.send(user, preset.name());
    } else {
      Message.PRESET_BIND_FAIL.send(user, preset.name());
    }
  }

  private void onModifierAdd(T sender, AttributeModifier modifier, User user) {
    user.addAttribute(modifier);
    recalculate(user);
    Message.MODIFIER_ADD.send(sender, user.name());
  }

  private void onModifierClear(T sender, User user) {
    user.clearAttributes();
    recalculate(user);
    Message.MODIFIER_CLEAR.send(sender, user.name());
  }

  private void recalculate(User user) {
    game.abilityManager(user.worldKey()).userInstances(user).forEach(Ability::loadConfig);
  }

  private AbilityDisplay collectAbilities(Audience user, Element element) {
    var components = Registries.ABILITIES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(desc -> !desc.isActivatedBy(Activation.SEQUENCE) && !desc.isActivatedBy(Activation.PASSIVE))
      .filter(desc -> hasPermission(user, desc))
      .map(AbilityDescription::meta)
      .toList();
    return new AbilityDisplay(Message.ABILITIES.build(), components);
  }

  private AbilityDisplay collectSequences(Audience user, Element element) {
    var components = Registries.SEQUENCES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden())
      .filter(desc -> hasPermission(user, desc))
      .map(AbilityDescription::meta)
      .toList();
    return new AbilityDisplay(Message.SEQUENCES.build(), components);
  }

  private AbilityDisplay collectPassives(Audience user, Element element) {
    var components = Registries.ABILITIES.stream()
      .filter(desc -> element == desc.element() && !desc.hidden() && desc.isActivatedBy(Activation.PASSIVE))
      .filter(desc -> hasPermission(user, desc))
      .map(AbilityDescription::meta)
      .toList();
    return new AbilityDisplay(Message.PASSIVES.build(), components);
  }

  private boolean hasPermission(Audience user, AbilityDescription desc) {
    PermissionChecker checker = user.getOrDefault(PermissionChecker.POINTER, PermissionChecker.always(TriState.NOT_SET));
    return desc.permissions().stream().allMatch(checker);
  }

  private static final class AbilityDisplay implements Iterable<Component> {
    private final Collection<Component> display;

    private AbilityDisplay(Component header, Collection<Component> abilities) {
      if (abilities.isEmpty()) {
        display = List.of();
      } else {
        JoinConfiguration sep = JoinConfiguration.commas(true);
        Component component = Component.join(sep, abilities).colorIfAbsent(ColorPalette.TEXT_COLOR);
        display = List.of(header, component.hoverEvent(HoverEvent.showText(Message.ABILITY_HOVER.build())));
      }
    }

    private boolean isEmpty() {
      return display.isEmpty();
    }

    @Override
    public Iterator<Component> iterator() {
      return display.iterator();
    }
  }
}
