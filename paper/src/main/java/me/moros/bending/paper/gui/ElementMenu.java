/*
 * Copy               right 2020-2024 Moros
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

package me.moros.bending.paper.gui;

import java.util.Map;

import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.type.ChestGui;
import com.github.stefvanschie.inventoryframework.pane.OutlinePane;
import com.github.stefvanschie.inventoryframework.pane.Pane.Priority;
import com.github.stefvanschie.inventoryframework.pane.StaticPane;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.ability.element.ElementHandler;
import me.moros.bending.api.gui.ElementGui;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.bending.common.gui.AbstractGui;
import me.moros.bending.common.locale.Message;
import me.moros.bending.paper.platform.PlatformAdapter;
import net.kyori.adventure.util.TriState;
import org.bukkit.inventory.ItemStack;

public final class ElementMenu extends AbstractGui<ItemStack, ChestGui> {
  private ElementMenu(ElementHandler handler, Player player) {
    super(handler, player);
  }

  @Override
  protected ChestGui construct(Map<Element, ItemStack> elementMap) {
    ChestGui gui = new ChestGui(3, Message.ELEMENTS_GUI_TITLE.build());
    gui.setOnGlobalClick(event -> event.setCancelled(true));
    OutlinePane background = new OutlinePane(0, 0, 9, 3, Priority.LOWEST);
    background.addItem(new GuiItem(backgroundItem()));
    background.setRepeat(true);
    gui.addPane(background);
    OutlinePane elementsPane = new OutlinePane(1, 1, 7, 1);
    elementsPane.setGap(1);
    User user = Registries.BENDERS.getOrThrow(player().uuid());
    for (Element element : Element.VALUES) {
      var data = createElementButton(element);
      var item = PlatformAdapter.toBukkitItem(data.item());
      handleItemStackGlow(item, user.hasElement(element));
      elementMap.put(element, item);
      elementsPane.addItem(new GuiItem(item, event -> {
        var click = event.getClick();
        ActionType action = mapType(click.isShiftClick(), click.isLeftClick(), click.isRightClick());
        if (action != null) {
          update(handleAction(action, data));
        }
      }));
    }
    gui.addPane(elementsPane);
    StaticPane helpPane = new StaticPane(0, 0, 9, 1);
    var item = PlatformAdapter.toBukkitItem(generateHelpItem());
    helpPane.addItem(new GuiItem(item), 4, 0);
    gui.addPane(helpPane);
    return gui;
  }

  private ItemStack backgroundItem() {
    ItemStack item = PlatformAdapter.toBukkitItem(BACKGROUND.get());
    item.editMeta(m -> m.setHideTooltip(true));
    return item;
  }

  @Override
  public boolean show(Player player) {
    handle().show(PlatformAdapter.toBukkitEntity(player));
    return true;
  }

  private void update(TriState result) {
    switch (result) {
      case TRUE -> handle().update();
      case FALSE -> close();
    }
  }

  private void close() {
    var bukkitPlayer = PlatformAdapter.toBukkitEntity(player());
    bukkitPlayer.closeInventory();
    bukkitPlayer.updateInventory();
  }

  @Override
  protected void handleItemStackGlow(ItemStack itemStack, boolean glow) {
    itemStack.editMeta(m -> m.setEnchantmentGlintOverride(glow));
  }

  public static ElementGui createMenu(ElementHandler handler, Player player) {
    return new ElementMenu(handler, player);
  }
}
