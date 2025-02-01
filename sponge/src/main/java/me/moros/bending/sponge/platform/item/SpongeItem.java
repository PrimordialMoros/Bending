/*
 * Copyright 2020-2025 Moros
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

import java.util.Optional;

import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.sponge.platform.PlatformAdapter;
import org.spongepowered.api.item.inventory.ItemStack;

public record SpongeItem(Item type, int amount, ItemStack handle) implements ItemSnapshot {
  public SpongeItem(ItemStack handle) {
    this(PlatformAdapter.fromSpongeItem(handle.type()), handle.quantity(), handle);
  }

  @Override
  public <T> Optional<T> get(DataKey<T> key) {
    return handle().get(PlatformAdapter.dataKey(key));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    return handle.equals(((SpongeItem) obj).handle);
  }

  @Override
  public int hashCode() {
    return handle.hashCode();
  }
}
