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

package me.moros.bending.platform.item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.util.metadata.Metadata;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class BukkitItemBuilder implements ItemBuilder {
  private final Material material;
  private final Set<Key> meta = new HashSet<>();
  private Component name;
  private List<Component> lore;
  private boolean unbreakable;

  public BukkitItemBuilder(Item item) {
    var material = PlatformAdapter.ITEM_MATERIAL_INDEX.valueOrThrow(item);
    if (!material.isItem()) {
      throw new IllegalStateException(material.name() + " is not an item!");
    }
    this.material = material;
  }

  @Override
  public ItemBuilder name(Component name) {
    this.name = name;
    return this;
  }

  @Override
  public ItemBuilder lore(List<Component> lore) {
    this.lore = lore;
    return this;
  }

  @Override
  public ItemBuilder meta(Key key) {
    this.meta.add(key);
    return this;
  }

  @Override
  public ItemBuilder unbreakable(boolean unbreakable) {
    this.unbreakable = unbreakable;
    return this;
  }

  @Override
  public ItemSnapshot build(int amount) {
    if (amount <= 0) {
      throw new IllegalStateException("Non positive amount: " + amount);
    }
    ItemStack stack = new ItemStack(material, amount);
    stack.editMeta(m -> {
      m.displayName(name);
      m.lore(lore);
      m.setUnbreakable(unbreakable);
      var data = m.getPersistentDataContainer();
      for (Key key : meta) {
        data.set(PlatformAdapter.nsk(key), PersistentDataType.BYTE, Metadata.EMPTY);
      }
    });
    return new BukkitItem(stack);
  }
}
