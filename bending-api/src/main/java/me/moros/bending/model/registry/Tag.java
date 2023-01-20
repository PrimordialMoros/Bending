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

package me.moros.bending.model.registry;

import java.util.Locale;

import me.moros.bending.util.KeyUtil;
import net.kyori.adventure.key.Key;

public interface Tag<V> extends Container<V> {
  boolean isTagged(Key key);

  default boolean isTagged(String value) {
    Key key = KeyUtil.VANILLA_KEY_MAPPER.apply(value.toLowerCase(Locale.ROOT));
    return key != null && isTagged(key);
  }

  default boolean isTagged(V type) {
    return containsValue(type);
  }
}
