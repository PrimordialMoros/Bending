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

package me.moros.bending.util.metadata;

import me.moros.bending.util.KeyUtil;
import net.kyori.adventure.key.Key;

/**
 * Utility class to provide metadata keys
 */
public final class Metadata {
  public static final Key NO_PICKUP = KeyUtil.simple("bending-no-pickup");
  public static final Key GLOVE_KEY = KeyUtil.simple("bending-earth-glove");
  public static final Key METAL_CABLE = KeyUtil.simple("bending-metal-cable");
  public static final Key DESTRUCTIBLE = KeyUtil.simple("bending-destructible");
  public static final Key NPC = KeyUtil.simple("bending-npc");

  public static final Key ARMOR_KEY = KeyUtil.simple("bending-armor");
  public static final Key METAL_KEY = KeyUtil.simple("bending-metal-key");

  public static final byte EMPTY = 0x1;

  private Metadata() {
  }
}
