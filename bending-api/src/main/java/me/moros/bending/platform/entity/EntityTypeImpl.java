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

package me.moros.bending.platform.entity;

import me.moros.bending.model.registry.Registry;
import me.moros.bending.util.KeyUtil;
import net.kyori.adventure.key.Key;

record EntityTypeImpl(Key key) implements EntityType {
  static final Registry<Key, EntityType> REGISTRY = Registry.vanilla("entities");

  static EntityType get(String key) {
    EntityType instance = new EntityTypeImpl(KeyUtil.vanilla(key));
    REGISTRY.register(instance);
    return instance;
  }
}
