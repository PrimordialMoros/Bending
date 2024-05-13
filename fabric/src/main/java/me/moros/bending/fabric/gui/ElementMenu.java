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

package me.moros.bending.fabric.gui;

import java.util.Map;

import eu.pb4.sgui.api.gui.SimpleGui;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.gui.AbstractGui;
import me.moros.bending.common.locale.Message;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.util.TriState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public final class ElementMenu extends AbstractGui<ItemStack, SimpleGui> {
  private ElementMenu(ElementHandler handler, Player player) {
    super(handler, player);
  }

  @Override
  protected SimpleGui construct(Map<Element, ItemStack> elementMap) {
    var player = PlatformAdapter.toFabricEntity(player());
    SimpleGui gui = new SimpleGui(MenuType.GENERIC_9x3, player, false);
    gui.setTitle(FabricServerAudiences.of(player.server).toNative(Message.ELEMENTS_GUI_TITLE.build()));
    var fill = PlatformAdapter.toFabricItem(BACKGROUND.get());
    fill.set(DataComponents.HIDE_TOOLTIP, Unit.INSTANCE);
    for (int i = 0; i < gui.getSize(); i++) {
      gui.setSlot(i, fill);
    }
    gui.setSlot(4, PlatformAdapter.toFabricItem(generateHelpItem()));
    int offset = 10;
    User user = Registries.BENDERS.getOrThrow(player().uuid());
    for (Element element : Element.VALUES) {
      var data = createElementButton(element);
      var item = PlatformAdapter.toFabricItem(data.item());
      handleItemStackGlow(item, user.hasElement(element));
      elementMap.put(element, item);
      gui.setSlot(offset, item, (idx, ct, ct2, g) -> {
        ActionType action = mapType(ct.shift, ct.isLeft, ct.isRight);
        if (action != null && handleAction(action, data) == TriState.FALSE) {
          handle().close();
        }
      });
      offset += 2;
    }
    return gui;
  }

  @Override
  public boolean show(Player player) {
    return handle().open();
  }

  @Override
  protected void handleItemStackGlow(ItemStack itemStack, boolean glow) {
    itemStack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, glow);
  }

  public static ElementGui createMenu(ElementHandler handler, Player player) {
    return new ElementMenu(handler, player);
  }
}
