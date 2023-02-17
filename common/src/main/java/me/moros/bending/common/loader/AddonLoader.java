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

package me.moros.bending.common.loader;

import java.nio.file.Path;

import me.moros.bending.api.addon.Addon;
import me.moros.bending.api.game.Game;

public interface AddonLoader extends Iterable<Addon> {
  default void loadAll() {
    for (var addon : this) {
      addon.load();
    }
  }

  default void enableAll(Game game) {
    for (var addon : this) {
      addon.enable(game);
    }
  }

  default void unloadAll() {
    for (var addon : this) {
      addon.unload();
    }
  }

  static AddonLoader create(Path dir, ClassLoader parent) {
    return new AddonLoaderImpl(new AddonClassLoader(parent).loadJars(dir.resolve("addons")));
  }
}
