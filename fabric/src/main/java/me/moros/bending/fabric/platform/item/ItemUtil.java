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

package me.moros.bending.fabric.platform.item;

import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.fabric.platform.NBTUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jspecify.annotations.Nullable;

public class ItemUtil {
  public static <T> @Nullable T getKey(ItemStack stack, DataKey<T> key) {
    CustomData data = stack.get(DataComponents.CUSTOM_DATA);
    return data == null ? null : NBTUtil.read(data.copyTag(), key); // TODO avoid copy?
  }

  public static <T> void addKey(ItemStack stack, DataKey<T> key, T value) {
    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> NBTUtil.write(tag, key, value));
  }

  public static void removeKey(ItemStack stack, DataKey<?> key) {
    CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(key.asString()));
  }
}
