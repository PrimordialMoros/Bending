/*
 * Copyright 2020-2026 Moros
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
import me.moros.bending.fabric.platform.FabricPersistentDataHolder;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class FabricItemBuilder implements ItemBuilder {
  private final ItemStack itemStack;
  private final DataComponentPatch.Builder patchBuilder;
  private final Map<DataKey<?>, Object> meta = new HashMap<>();
  private final MinecraftServer server;
  private final MinecraftServerAudiences adapter;

  public FabricItemBuilder(ItemStack itemStack, MinecraftServer server) {
    this.itemStack = itemStack;
    this.patchBuilder = DataComponentPatch.builder();
    this.server = server;
    this.adapter = MinecraftServerAudiences.of(server);
  }

  @Override
  public ItemBuilder name(Component name) {
    patchBuilder.set(DataComponents.CUSTOM_NAME, adapter.asNative(name));
    return this;
  }

  @Override
  public ItemBuilder lore(List<Component> lore) {
    patchBuilder.set(DataComponents.LORE, new ItemLore(lore.stream().map(adapter::asNative).toList()));
    return this;
  }

  @Override
  public <T> ItemBuilder meta(DataKey<T> key, T value) {
    this.meta.put(key, value);
    return this;
  }

  @Override
  public ItemBuilder unbreakable(boolean unbreakable) {
    if (unbreakable) {
      patchBuilder.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    } else {
      patchBuilder.remove(DataComponents.UNBREAKABLE);
    }
    return this;
  }

  @Override
  public ItemBuilder boundArmor() {
    patchBuilder.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    var bindingCurse = server.registryAccess()
      .lookupOrThrow(Registries.ENCHANTMENT)
      .getOrThrow(Enchantments.BINDING_CURSE);
    ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
    enchantments.set(bindingCurse, 1);
    patchBuilder.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
    patchBuilder.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
    patchBuilder.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT
      .withHidden(DataComponents.ENCHANTMENTS, true));
    return this;
  }

  @Override
  public ItemSnapshot build(int amount) {
    if (amount <= 0) {
      throw new IllegalStateException("Non positive amount: " + amount);
    }
    ItemStack stack = itemStack.copyWithCount(amount);
    stack.applyComponents(patchBuilder.build());
    var store = FabricPersistentDataHolder.create(stack);
    for (var entry : meta.entrySet()) {
      addMeta(store, entry.getKey(), entry.getValue()); // Get around type erasure
    }
    return FabricItem.createFabricItem(stack);
  }

  @SuppressWarnings("unchecked")
  private static <T> void addMeta(DataHolder store, DataKey<T> key, Object value) {
    store.add(key, (T) value);
  }
}
