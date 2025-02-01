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

package me.moros.bending.api.protection;

import java.util.Locale;

import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;

/**
 * Base class for {@link Protection}.
 */
public abstract class AbstractProtection implements Protection {
  private final Key key;

  private AbstractProtection(Key key) {
    this.key = key;
  }

  protected AbstractProtection(String protectionName) {
    this(KeyUtil.simple(protectionName.toLowerCase(Locale.ROOT)));
  }

  protected AbstractProtection(String namespace, String protectionName) {
    this(Key.key(namespace, protectionName));
  }

  @Override
  public Key key() {
    return key;
  }
}
