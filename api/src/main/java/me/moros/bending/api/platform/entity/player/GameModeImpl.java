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

package me.moros.bending.api.platform.entity.player;

import me.moros.bending.api.registry.DefaultedRegistry;
import me.moros.bending.api.registry.Registry;
import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;

record GameModeImpl(Key key) implements GameMode {
  static final DefaultedRegistry<Key, GameMode> REGISTRY = Registry.vanillaDefaulted("gamemode", GameMode.class, GameModeImpl::new);

  static GameMode get(String key) {
    return REGISTRY.get(KeyUtil.vanilla(key));
  }
}
