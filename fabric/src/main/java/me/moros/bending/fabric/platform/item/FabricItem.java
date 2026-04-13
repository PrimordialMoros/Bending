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

import java.util.Optional;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.metadata.Metadata;
import me.moros.bending.fabric.platform.PlatformAdapter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public final class FabricItem implements ItemSnapshot {
  private static final ItemSnapshot EMPTY = new EmptyFabricItem();

  private final Item type;
  private final ItemStackTemplate handle;

  private FabricItem(ItemStack handle) {
    this.type = PlatformAdapter.fromFabricItem(handle.typeHolder().value());
    this.handle = ItemStackTemplate.fromNonEmptyStack(handle);
  }

  public ItemStackTemplate asTemplate() {
    return handle;
  }

  @Override
  public Item type() {
    return type;
  }

  @Override
  public int amount() {
    return handle.count();
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return Metadata.isPersistent(key) ? Optional.ofNullable(ItemUtil.getKey(handle, key)) : Optional.empty();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return handle.equals(((FabricItem) obj).handle);
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }

  public static ItemSnapshot createFabricItem(ItemStack handle) {
    return handle.isEmpty() ? EMPTY : new FabricItem(handle);
  }

  public static boolean isEmpty(ItemSnapshot item) {
    return EMPTY.equals(item);
  }
}
