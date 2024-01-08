/*
 * Copyright 2020-2024 Moros
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
import java.util.Collection;
import java.util.function.Supplier;

import me.moros.bending.api.addon.Addon;
import me.moros.bending.api.game.Game;
import me.moros.bending.common.logging.Logger;

public interface AddonLoader extends Iterable<Addon> {
  void loadAll(Collection<Supplier<Addon>> addonProviders);

  void enableAll(Game game);

  void unloadAll();

  static AddonLoader create(Logger logger, Path dir, ClassLoader parent) {
    return new AddonLoaderImpl(logger, new AddonClassLoader(parent).loadJars(dir.resolve("addons")));
  }
}
