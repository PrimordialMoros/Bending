/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.bending.Bending;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PersistentDataLayer {
	private static final byte VALUE = (byte) 0x1;

	public static final String STR_ARMOR = "bending-armor";
	public static final String STR_MATERIAL = "bending-material";

	private final NamespacedKey NSK_ARMOR;
	private final NamespacedKey NSK_MATERIAL;

	public PersistentDataLayer(@NonNull Bending plugin) {
		NSK_ARMOR = new NamespacedKey(plugin, STR_ARMOR);
		NSK_MATERIAL = new NamespacedKey(plugin, STR_MATERIAL);
	}

	public boolean hasArmorKey(@Nullable ItemMeta meta) {
		if (meta == null) return false;
		PersistentDataContainer container = meta.getPersistentDataContainer();
		return container.has(NSK_ARMOR, PersistentDataType.BYTE);
	}

	public boolean addArmorKey(@NonNull ItemMeta meta) {
		if (meta != null) {
			PersistentDataContainer container = meta.getPersistentDataContainer();
			if (!hasArmorKey(meta)) {
				container.set(NSK_ARMOR, PersistentDataType.BYTE, VALUE);
				return true;
			}
		}
		return false;
	}

	public @NonNull NamespacedKey getMaterialKey() {
		return NSK_MATERIAL;
	}
}
