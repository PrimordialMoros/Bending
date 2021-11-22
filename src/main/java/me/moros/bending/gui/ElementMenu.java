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

package me.moros.bending.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;

import com.github.stefvanschie.inventoryframework.adventuresupport.ComponentHolder;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.gui.type.util.Gui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.Pane.Priority;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.moros.bending.command.BendingCommand;
import me.moros.bending.locale.Message;
import me.moros.bending.locale.Message.Args0;
import me.moros.bending.model.Element;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ChatUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ElementMenu implements BendingMenu {
  private static final ChestGui BASE_GUI;
  private static final UUID innerPaneUUID;

  static {
    BASE_GUI = new ChestGui(3, ComponentHolder.of(Message.ELEMENTS_GUI_TITLE.build()));
    BASE_GUI.setOnGlobalClick(event -> event.setCancelled(true));
    OutlinePane background = new OutlinePane(0, 0, 9, 3, Priority.LOWEST);

    ItemStack backgroundItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    ItemMeta backgroundItemMeta = backgroundItem.getItemMeta();
    backgroundItemMeta.displayName(Component.empty());
    backgroundItem.setItemMeta(backgroundItemMeta);

    background.addItem(new GuiItem(backgroundItem));
    background.setRepeat(true);

    BASE_GUI.addPane(background);

    OutlinePane elementsPane = new OutlinePane(1, 1, 7, 1);
    innerPaneUUID = elementsPane.getUUID();
    elementsPane.setGap(1);
    BASE_GUI.addPane(elementsPane);
  }

  public ElementMenu(@NonNull BendingPlayer player) {
    if (!player.hasPermission(permission())) {
      Message.GUI_NO_PERMISSION.send(player);
      return;
    }

    ChestGui gui = BASE_GUI.copy();
    OutlinePane pane = gui.getPanes().stream().filter(p -> p.getUUID().equals(innerPaneUUID) && p instanceof OutlinePane)
      .map(OutlinePane.class::cast).findAny().orElseThrow(() -> new RuntimeException("Gui cloning went wrong!"));

    for (Element element : Element.VALUES) {
      ItemStack itemStack = generateItem(player, element);
      DataWrapper data = new DataWrapper(player, element, itemStack, gui);
      pane.addItem(new GuiItem(itemStack, event -> handleClick(event, data)));
    }

    StaticPane helpPane = new StaticPane(0, 0, 9, 1);
    helpPane.addItem(new GuiItem(generateHelpItem(player)), 4, 0);
    gui.addPane(helpPane);
    gui.show(player.entity());
  }

  @Override
  public @NonNull String permission() {
    return "bending.command.help";
  }

  private static void handleClick(InventoryClickEvent event, DataWrapper meta) {
    Player player = meta.player.entity();
    Element element = meta.element;
    ActionType type = mapType(event.getClick());
    if (type != null && type.executeCommand(meta.player, element)) {
      if (type.keepOpen()) {
        handleItemStackGlow(meta.player(), element, meta.itemStack);
        meta.gui.update();
      } else {
        player.closeInventory();
        player.updateInventory();
      }
    }
  }

  private static ActionType mapType(ClickType clickType) {
    if (clickType.isShiftClick()) {
      if (clickType.isLeftClick()) {
        return ActionType.ADD;
      } else if (clickType.isRightClick()) {
        return ActionType.REMOVE;
      }
    } else {
      if (clickType.isLeftClick()) {
        return ActionType.CHOOSE;
      } else if (clickType.isRightClick()) {
        return ActionType.DISPLAY;
      }
    }
    return null;
  }

  private static void handleItemStackGlow(BendingPlayer player, Element element, ItemStack itemStack) {
    if (player.hasElement(element)) {
      itemStack.addUnsafeEnchantment(Enchantment.LUCK, 1);
    } else {
      itemStack.removeEnchantment(Enchantment.LUCK);
    }
  }

  private static ItemStack generateItem(BendingPlayer player, Element element) {
    ItemStack itemStack = new ItemStack(mapElement(element));
    itemStack.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    handleItemStackGlow(player, element, itemStack);
    ItemMeta itemMeta = itemStack.getItemMeta();
    Component itemName = GlobalTranslator.render(element.displayName(), player.entity().locale())
      .decoration(TextDecoration.ITALIC, false);
    itemMeta.displayName(itemName);
    itemMeta.lore(generateLore(player.entity(), element));
    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  private static List<Component> generateLore(Player player, Element element) {
    Component base = GlobalTranslator.render(element.description(), player.locale());
    String raw = PlainTextComponentSerializer.plainText().serialize(base);
    List<Component> lore = new ArrayList<>();
    for (String s : ChatUtil.wrap(raw, 36)) {
      lore.add(Component.text(s, element.color()));
    }
    return lore;
  }

  private static ItemStack generateHelpItem(BendingPlayer player) {
    ItemStack itemStack = new ItemStack(Material.BOOK);
    ItemMeta itemMeta = itemStack.getItemMeta();
    // ItemStacks get translated early, so we render in default locale for now
    Component itemName = GlobalTranslator.render(Message.ELEMENTS_GUI_HELP_TITLE.build(), player.entity().locale())
      .decoration(TextDecoration.ITALIC, false);
    itemMeta.displayName(itemName);
    itemMeta.lore(ActionType.VALUES.stream().map(type -> type.toEntry(player)).toList());
    itemStack.setItemMeta(itemMeta);
    return itemStack;
  }

  private static Material mapElement(Element element) {
    return switch (element) {
      case AIR -> Material.WHITE_CONCRETE;
      case WATER -> Material.LIGHT_BLUE_CONCRETE;
      case EARTH -> Material.GREEN_CONCRETE;
      case FIRE -> Material.RED_CONCRETE;
    };
  }

  private static record DataWrapper(BendingPlayer player, Element element, ItemStack itemStack, Gui gui) {
  }

  private enum ActionType {
    CHOOSE(Message.ELEMENTS_GUI_CHOOSE, "bending.command.choose", BendingCommand::onElementChoose, false),
    DISPLAY(Message.ELEMENTS_GUI_DISPLAY, "bending.command.display", BendingCommand::onElementDisplay, false),
    ADD(Message.ELEMENTS_GUI_ADD, "bending.command.add", BendingCommand::onElementAdd, true),
    REMOVE(Message.ELEMENTS_GUI_REMOVE, "bending.command.remove", BendingCommand::onElementRemove, true);

    private final Args0 message;
    private final String permission;
    private final BiConsumer<User, Element> commandConsumer;
    private final boolean keepOpen;

    ActionType(Args0 message, String permission, BiConsumer<User, Element> commandConsumer, boolean keepOpen) {
      this.message = message;
      this.permission = permission;
      this.commandConsumer = commandConsumer;
      this.keepOpen = keepOpen;
    }

    private boolean keepOpen() {
      return keepOpen;
    }

    private boolean executeCommand(User user, Element element) {
      if (!user.hasPermission(permission)) {
        Message.GUI_NO_PERMISSION.send(user);
        return false;
      }
      commandConsumer.accept(user, element);
      return true;
    }

    private Component toEntry(BendingPlayer player) {
      return GlobalTranslator.render(message.build(), player.entity().locale())
        .decoration(TextDecoration.ITALIC, false)
        .decoration(TextDecoration.STRIKETHROUGH, !player.hasPermission(permission));
    }

    private static final Collection<ActionType> VALUES = List.of(values());
  }
}
