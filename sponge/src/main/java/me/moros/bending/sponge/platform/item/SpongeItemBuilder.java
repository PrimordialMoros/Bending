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

package me.moros.bending.sponge.platform.item;

import java.util.List;

import me.moros.bending.api.platform.item.ItemBuilder;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.sponge.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import org.spongepowered.api.data.Keys;
import org.spongepowered.api.item.inventory.ItemStack;

public class SpongeItemBuilder implements ItemBuilder {
  private final ItemStack stack;

  public SpongeItemBuilder(ItemStack stack) {
    this.stack = stack;
  }

  @Override
  public ItemBuilder name(Component name) {
    stack.offer(Keys.CUSTOM_NAME, name);
    return this;
  }

  @Override
  public ItemBuilder lore(List<Component> lore) {
    stack.offer(Keys.LORE, lore);
    return this;
  }

  @Override
  public <T> ItemBuilder meta(DataKey<T> key, T value) {
    stack.offer(PlatformAdapter.dataKey(key), value);
    return this;
  }

  @Override
  public ItemBuilder unbreakable(boolean unbreakable) {
    stack.offer(Keys.IS_UNBREAKABLE, unbreakable);
    return this;
  }

  @Override
  public ItemSnapshot build(int amount) {
    if (amount <= 0) {
      throw new IllegalStateException("Non positive amount: " + amount);
    }
    stack.setQuantity(amount);
    return new SpongeItem(stack);
  }
}
