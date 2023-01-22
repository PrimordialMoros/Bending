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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.moros.bending.model.data.DataKey;
import me.moros.bending.platform.PlatformAdapter;
import me.moros.bending.util.metadata.Metadata;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;

public class BukkitItemBuilder implements ItemBuilder {
  private final ItemStack stack;
  private final Map<DataKey<?>, Object> meta = new HashMap<>();
  private Component name;
  private List<Component> lore;
  private boolean unbreakable;

  public BukkitItemBuilder(ItemStack stack) {
    this.stack = stack;
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
  public <T> ItemBuilder meta(DataKey<T> key, T value) {
    if (Metadata.isPersistent(key)) {
      this.meta.put(key, value);
    }
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
    var copy = stack.clone();
    copy.setAmount(amount);
    copy.editMeta(m -> {
      m.displayName(name);
      m.lore(lore);
      m.setUnbreakable(unbreakable);
      var data = m.getPersistentDataContainer();
      for (var entry : meta.entrySet()) {
        addMeta(data, entry.getKey(), entry.getValue()); // Get around type erasure
      }
    });
    return new BukkitItem(copy);
  }

  @SuppressWarnings("unchecked")
  private static <T> void addMeta(PersistentDataContainer store, DataKey<T> key, Object value) {
    var type = PlatformAdapter.dataType(key);
    if (type != null) {
      store.set(PlatformAdapter.nsk(key), type, (T) value);
    }
  }
}
