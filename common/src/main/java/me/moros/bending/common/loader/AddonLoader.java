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
import java.util.Collection;
import java.util.function.Consumer;

import me.moros.bending.api.addon.Addon;
import me.moros.bending.api.game.Game;
import me.moros.bending.common.logging.Logger;

public interface AddonLoader extends Iterable<Addon> {
  Logger logger();

  default void loadAll() {
    forEachSafe(Addon::load);
  }

  default void enableAll(Game game) {
    forEachSafe(addon -> addon.enable(game));
  }

  default void unloadAll() {
    forEachSafe(Addon::unload);
  }

  private void forEachSafe(Consumer<Addon> addonConsumer) {
    for (var addon : this) {
      try {
        addonConsumer.accept(addon);
      } catch (Throwable t) {
        logger().warn(t.getMessage(), t);
      }
    }
  }

  static AddonLoader create(Logger logger, Path dir, ClassLoader parent, Collection<Addon> addons) {
    return new AddonLoaderImpl(logger, new AddonClassLoader(parent).loadJars(dir.resolve("addons")), addons);
  }
}
