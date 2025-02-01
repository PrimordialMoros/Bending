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

package me.moros.bending.paper.platform.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.Unbreakable;
import me.moros.bending.api.locale.Translation;
import me.moros.bending.api.platform.item.ItemBuilder;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.metadata.Metadata;
import me.moros.bending.paper.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.checkerframework.checker.nullness.qual.Nullable;

public class BukkitItemBuilder implements ItemBuilder {
  private final ItemStack stack;
  private final Map<DataKey<?>, Object> meta = new HashMap<>();

  public BukkitItemBuilder(ItemStack stack) {
    this.stack = stack.clone();
  }

  // TODO remove when components are properly supported
  private @Nullable Component tryRender(@Nullable Component input) {
    return input == null ? null : GlobalTranslator.render(input, Translation.DEFAULT_LOCALE);
  }

  @Override
  public ItemBuilder name(Component name) {
    this.stack.setData(DataComponentTypes.CUSTOM_NAME, tryRender(name));
    return this;
  }

  @Override
  public ItemBuilder lore(List<Component> lore) {
    this.stack.setData(DataComponentTypes.LORE, ItemLore.lore(lore.stream().map(this::tryRender).toList()));
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
    if (unbreakable) {
      stack.setData(DataComponentTypes.UNBREAKABLE, Unbreakable.unbreakable(false));
    } else {
      stack.unsetData(DataComponentTypes.UNBREAKABLE);
    }
    return this;
  }

  @Override
  public ItemSnapshot build(int amount) {
    if (amount <= 0) {
      throw new IllegalStateException("Non positive amount: " + amount);
    }
    stack.setAmount(amount);
    if (!meta.isEmpty()) {
      stack.editMeta(m -> {
        var data = m.getPersistentDataContainer();
        for (var entry : meta.entrySet()) {
          addMeta(data, entry.getKey(), entry.getValue()); // Get around type erasure
        }
      });
    }
    return new BukkitItem(stack);
  }

  @SuppressWarnings("unchecked")
  private static <T> void addMeta(PersistentDataContainer store, DataKey<T> key, Object value) {
    var type = PlatformAdapter.dataType(key);
    if (type != null) {
      store.set(PlatformAdapter.nsk(key), type, (T) value);
    }
  }
}
