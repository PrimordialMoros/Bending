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

package me.moros.bending.fabric.platform.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.moros.bending.api.platform.item.ItemBuilder;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.data.DataHolder;
import me.moros.bending.api.util.data.DataKey;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;

public class FabricItemBuilder implements ItemBuilder {
  private final Map<DataKey<?>, Object> meta = new HashMap<>();
  private final ItemStack stack;
  private final MinecraftServer server;

  public FabricItemBuilder(ItemStack stack, MinecraftServer server) {
    this.stack = stack;
    this.server = server;
  }

  @Override
  public ItemBuilder name(Component name) {
    stack.setHoverName(FabricServerAudiences.of(server).toNative(name));
    return this;
  }

  @Override
  public ItemBuilder lore(List<Component> lore) {
    ItemUtil.setLore(stack, lore);
    return this;
  }

  @Override
  public <T> ItemBuilder meta(DataKey<T> key, T value) {
    this.meta.put(key, value);
    return this;
  }

  @Override
  public ItemBuilder unbreakable(boolean unbreakable) {
    ItemUtil.setUnbreakable(stack, unbreakable);
    return this;
  }

  @Override
  public ItemSnapshot build(int amount) {
    if (amount <= 0) {
      throw new IllegalStateException("Non positive amount: " + amount);
    }
    stack.setCount(amount);
    var fabricItem = new FabricItem(stack);
    for (var entry : meta.entrySet()) {
      addMeta(fabricItem, entry.getKey(), entry.getValue()); // Get around type erasure
    }
    return fabricItem;
  }

  @SuppressWarnings("unchecked")
  private static <T> void addMeta(DataHolder store, DataKey<T> key, Object value) {
    store.add(key, (T) value);
  }
}
