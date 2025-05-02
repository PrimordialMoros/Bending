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

package me.moros.bending.api.util.metadata;

import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;

/**
 * Utility class to provide metadata keys
 */
public final class Metadata {
  public static final DataKey<Boolean> NPC = KeyUtil.data("npc", Boolean.class);

  public static final DataKey<Boolean> ARMOR_KEY = KeyUtil.data("armor", Boolean.class);
  @Deprecated(forRemoval = true, since = "3.12.0")
  public static final DataKey<String> METAL_KEY = KeyUtil.data("bending-metal-key", String.class);

  private Metadata() {
  }

  // Maybe use registry
  public static boolean isPersistent(DataKey<?> key) {
    return key == ARMOR_KEY;
  }
}
