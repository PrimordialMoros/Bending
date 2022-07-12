/*
 * Copyright 2020-2022 Moros
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

import java.util.Objects;

import org.bukkit.NamespacedKey;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to provide metadata keys
 * @see FixedMetadataValue
 */
public final class Metadata {
  public static final String NO_PICKUP = "bending-no-pickup";
  public static final String GLOVE_KEY = "bending-earth-glove";
  public static final String METAL_CABLE = "bending-metal-cable";
  public static final String DESTRUCTIBLE = "bending-destructible";
  public static final String HIDDEN_BOARD = "bending-hidden-board";
  public static final String NPC = "bending-npc";

  public static final String PERSISTENT_ARMOR = "bending-armor";
  public static final String PERSISTENT_MATERIAL = "bending-material";

  public static final NamespacedKey NSK_ARMOR = new NamespacedKey("bending", PERSISTENT_ARMOR);
  public static final NamespacedKey NSK_MATERIAL = new NamespacedKey("bending", PERSISTENT_MATERIAL);

  private static final byte VALUE = 0x1;

  private static Plugin plugin;

  private Metadata() {
  }

  public static void inject(Plugin plugin) {
    Objects.requireNonNull(plugin);
    if (Metadata.plugin == null) {
      Metadata.plugin = plugin;
    }
  }

  public static boolean hasArmorKey(@Nullable PersistentDataHolder dataHolder) {
    if (dataHolder == null) {
      return false;
    }
    return dataHolder.getPersistentDataContainer().has(NSK_ARMOR, PersistentDataType.BYTE);
  }

  public static boolean addArmorKey(PersistentDataHolder dataHolder) {
    if (!hasArmorKey(dataHolder)) {
      dataHolder.getPersistentDataContainer().set(NSK_ARMOR, PersistentDataType.BYTE, VALUE);
      return true;
    }
    return false;
  }

  public static void add(Metadatable target, String key) {
    add(target, key, null);
  }

  public static void add(Metadatable target, String key, @Nullable Object object) {
    target.setMetadata(key, new FixedMetadataValue(plugin, object));
  }

  public static void remove(Metadatable target, String key) {
    target.removeMetadata(key, plugin);
  }
}
