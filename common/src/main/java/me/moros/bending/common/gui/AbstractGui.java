/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import cloud.commandframework.permission.CommandPermission;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.locale.Message.Args0;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.TextUtil;
import me.moros.bending.api.util.functional.Suppliers;
import me.moros.bending.common.command.CommandPermissions;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.TriState;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractGui<ItemStack, T> implements ElementGui {
  protected static final Supplier<ItemSnapshot> BACKGROUND = Suppliers.lazy(() ->
    Platform.instance().factory().itemBuilder(Item.BLACK_STAINED_GLASS_PANE)
      .name(Component.empty()).build()
  );

  private final Map<Element, ItemStack> map;
  private final ElementHandler handler;
  private final Player player;
  private final T gui;

  protected AbstractGui(ElementHandler handler, Player player) {
    this.map = new EnumMap<>(Element.class);
    this.handler = handler;
    this.player = player;
    this.gui = construct(map);
  }

  protected T handle() {
    return gui;
  }

  protected Player player() {
    return player;
  }

  protected abstract T construct(Map<Element, ItemStack> elementMap);

  protected DataWrapper createElementButton(Element element) {
    Component itemName = GlobalTranslator.render(element.displayName(), player.locale())
      .decoration(TextDecoration.ITALIC, false);
    List<Component> lore = generateLore(player.locale(), element);
    var item = Platform.instance().factory().itemBuilder(mapElement(element))
      .name(itemName).lore(lore).build();
    return new DataWrapper(element, item);
  }

  protected TriState handleAction(ActionType action, DataWrapper meta) {
    User user = Registries.BENDERS.get(player.uuid());
    if (user != null && action.executeCommand(handler, user, meta.element())) {
      if (action.keepOpen()) {
        handleItemStackGlow(map.get(meta.element()), user.hasElement(meta.element()));
        return TriState.TRUE;
      } else {
        return TriState.FALSE;
      }
    }
    return TriState.NOT_SET;
  }

  protected abstract void handleItemStackGlow(ItemStack itemStack, boolean glow);

  protected static @Nullable ActionType mapType(boolean shift, boolean left, boolean right) {
    if (shift) {
      if (left) {
        return ActionType.ADD;
      } else if (right) {
        return ActionType.REMOVE;
      }
    } else {
      if (left) {
        return ActionType.CHOOSE;
      } else if (right) {
        return ActionType.DISPLAY;
      }
    }
    return null;
  }

  protected ItemSnapshot generateHelpItem() {
    Component itemName = GlobalTranslator.render(Message.ELEMENTS_GUI_HELP_TITLE.build(), player.locale())
      .decoration(TextDecoration.ITALIC, false);
    List<Component> lore = ActionType.VALUES.stream().map(type -> type.toEntry(player)).toList();
    return Platform.instance().factory().itemBuilder(Item.BOOK)
      .name(itemName).lore(lore).build();
  }

  private static List<Component> generateLore(Locale locale, Element element) {
    Component base = GlobalTranslator.render(element.description(), locale);
    String raw = PlainTextComponentSerializer.plainText().serialize(base);
    List<Component> lore = new ArrayList<>();
    for (String s : TextUtil.wrap(raw, 36)) {
      lore.add(Component.text(s, element.color()));
    }
    return lore;
  }

  private static Item mapElement(Element element) {
    return switch (element) {
      case AIR -> Item.WHITE_CONCRETE;
      case WATER -> Item.LIGHT_BLUE_CONCRETE;
      case EARTH -> Item.GREEN_CONCRETE;
      case FIRE -> Item.RED_CONCRETE;
    };
  }

  public record DataWrapper(Element element, ItemSnapshot item) {
  }

  protected enum ActionType {
    CHOOSE(Message.ELEMENTS_GUI_CHOOSE, CommandPermissions.CHOOSE, ElementHandler::onElementChoose, false),
    DISPLAY(Message.ELEMENTS_GUI_DISPLAY, CommandPermissions.HELP, ElementHandler::onElementDisplay, false),
    ADD(Message.ELEMENTS_GUI_ADD, CommandPermissions.ADD, ElementHandler::onElementAdd, true),
    REMOVE(Message.ELEMENTS_GUI_REMOVE, CommandPermissions.REMOVE, ElementHandler::onElementRemove, true);

    private final Args0 message;
    private final String permission;
    private final MenuAction menuAction;
    private final boolean keepOpen;

    ActionType(Args0 message, CommandPermission permission, MenuAction menuAction, boolean keepOpen) {
      this.message = message;
      this.permission = permission.toString();
      this.menuAction = menuAction;
      this.keepOpen = keepOpen;
    }

    public boolean keepOpen() {
      return keepOpen;
    }

    public boolean executeCommand(ElementHandler handler, User user, Element element) {
      if (!user.hasPermission(permission)) {
        Message.GUI_NO_PERMISSION.send(user);
        return false;
      }
      menuAction.accept(handler, user, element);
      return true;
    }

    private Component toEntry(Audience audience) {
      boolean strikethrough = audience.get(PermissionChecker.POINTER)
        .map(checker -> !checker.test(permission)).orElse(false);
      return GlobalTranslator.render(message.build(), audience.getOrDefault(Identity.LOCALE, Message.DEFAULT_LOCALE))
        .decoration(TextDecoration.ITALIC, false)
        .decoration(TextDecoration.STRIKETHROUGH, strikethrough);
    }

    private static final Collection<ActionType> VALUES = List.of(values());
  }

  @FunctionalInterface
  protected interface MenuAction {
    void accept(ElementHandler handler, User user, Element element);
  }
}
