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

package me.moros.bending.sponge.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.locale.Message;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.user.BendingPlayer;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.common.gui.AbstractGui;
import me.moros.bending.sponge.platform.PlatformAdapter;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentTypes;
import org.spongepowered.api.item.inventory.ContainerTypes;
import org.spongepowered.api.item.inventory.Inventory;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.menu.ClickType;
import org.spongepowered.api.item.inventory.menu.ClickTypes;
import org.spongepowered.api.item.inventory.menu.InventoryMenu;
import org.spongepowered.api.item.inventory.type.ViewableInventory;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.plugin.PluginContainer;

public final class ElementMenu extends AbstractGui<ItemStack, InventoryMenu> {
  private static PluginContainer container;

  private ElementMenu(ElementHandler handler, BendingPlayer user) {
    super(handler, user);
  }

  @Override
  protected InventoryMenu construct(Map<Element, ItemStack> elementMap) {
    Inventory grid = Inventory.builder().slots(5).completeStructure().plugin(container).build();
    var slots = List.of(
      Vector2i.from(4, 0), // Help
      Vector2i.from(1, 1), Vector2i.from(3, 1), Vector2i.from(5, 1), Vector2i.from(7, 1) // Elements
    );
    ViewableInventory inv = ViewableInventory.builder().type(ContainerTypes.GENERIC_9X3)
      .fillDummy().item(PlatformAdapter.toSpongeItemSnapshot(BACKGROUND.get()))
      .slotsAtPositions(grid.slots(), slots)
      .completeStructure().plugin(container).build();
    InventoryMenu gui = inv.asMenu();
    gui.setReadOnly(true);
    gui.setTitle(Message.ELEMENTS_GUI_TITLE.build());
    inv.set(4, PlatformAdapter.toSpongeItem(generateHelpItem()));
    int offset = 10;
    Map<Integer, DataWrapper> dataMap = new HashMap<>();
    for (Element element : Element.VALUES) {
      var data = createElementButton(element);
      var item = PlatformAdapter.toSpongeItem(data.item());
      handleItemStackGlow(item, user().hasElement(element));
      elementMap.put(element, item);
      inv.set(offset, item);
      dataMap.put(offset, data);
      offset += 2;
    }
    gui.registerSlotClick((c, c2, slot, idx, click) -> {
      var action = mapType(isShiftClick(click), isLeftClick(click), isRightClick(click));
      var data = dataMap.get(idx);
      if (data != null && action != null) {
        switch (handleAction(action, data)) {
          case TRUE -> slot.set(elementMap.get(data.element()));
          case FALSE -> close();
        }
      }
      return false;
    });
    return gui;
  }

  @Override
  public boolean show(Player player) {
    handle().open(PlatformAdapter.toSpongeEntity(player));
    return true;
  }

  private void close() {
    var spongePlayer = PlatformAdapter.toSpongeEntity(user());
    Tasker.sync().submit(spongePlayer::closeInventory);
  }

  @Override
  protected void handleItemStackGlow(ItemStack itemStack, boolean glow) {
    itemStack.offer(Keys.HIDE_ENCHANTMENTS, true);
    if (glow) {
      itemStack.offerSingle(Keys.APPLIED_ENCHANTMENTS, Enchantment.of(EnchantmentTypes.LUCK_OF_THE_SEA, 1));
    } else {
      itemStack.removeSingle(Keys.APPLIED_ENCHANTMENTS, Enchantment.of(EnchantmentTypes.LUCK_OF_THE_SEA, 1));
    }
  }

  public static ElementGui createMenu(ElementHandler handler, BendingPlayer player) {
    return new ElementMenu(handler, player);
  }

  private static boolean isShiftClick(ClickType<?> click) {
    return click == ClickTypes.SHIFT_CLICK_LEFT.get()
      || click == ClickTypes.SHIFT_CLICK_RIGHT.get()
      || click == ClickTypes.KEY_THROW_ALL.get();
  }

  private static boolean isLeftClick(ClickType<?> click) {
    return click == ClickTypes.CLICK_LEFT.get()
      || click == ClickTypes.SHIFT_CLICK_LEFT.get()
      || click == ClickTypes.DOUBLE_CLICK.get()
      || click == ClickTypes.CLICK_MIDDLE.get();
  }

  private static boolean isRightClick(ClickType<?> click) {
    return click == ClickTypes.CLICK_RIGHT.get()
      || click == ClickTypes.SHIFT_CLICK_RIGHT.get();
  }
}
