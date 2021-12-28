/*
 * Copyright 2020-2021 Moros
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

package me.moros.bending.util.metadata;

import me.moros.bending.Bending;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PersistentDataLayer {
  private static final byte VALUE = 0x1;

  public static final String STR_ARMOR = "bending-armor";
  public static final String STR_MATERIAL = "bending-material";

  public final NamespacedKey NSK_ARMOR;
  public final NamespacedKey NSK_MATERIAL;

  public PersistentDataLayer(@NonNull Bending plugin) {
    NSK_ARMOR = new NamespacedKey(plugin, STR_ARMOR);
    NSK_MATERIAL = new NamespacedKey(plugin, STR_MATERIAL);
  }

  public boolean hasArmorKey(@Nullable PersistentDataHolder dataHolder) {
    if (dataHolder == null) {
      return false;
    }
    return dataHolder.getPersistentDataContainer().has(NSK_ARMOR, PersistentDataType.BYTE);
  }

  public boolean addArmorKey(@NonNull PersistentDataHolder dataHolder) {
    if (!hasArmorKey(dataHolder)) {
      dataHolder.getPersistentDataContainer().set(NSK_ARMOR, PersistentDataType.BYTE, VALUE);
      return true;
    }
    return false;
  }
}
