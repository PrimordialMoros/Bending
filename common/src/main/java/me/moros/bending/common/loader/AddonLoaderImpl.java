/*
 * Copyright 2020-2026 Moros
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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import me.moros.bending.api.addon.Addon;
import me.moros.bending.api.addon.BendingContext;
import me.moros.bending.api.game.Game;
import me.moros.bending.common.logging.Logger;

record AddonLoaderImpl(Logger logger, AddonClassLoader loader, BendingContext context,
                       Collection<Addon> addons) implements AddonLoader {
  AddonLoaderImpl(Logger logger, AddonClassLoader loader) {
    this(logger, loader, new BendingContextImpl(), ConcurrentHashMap.newKeySet());
  }

  @Override
  public void loadAll(Collection<Supplier<Addon>> providers) {
    Stream.concat(ServiceLoader.load(Addon.class, loader).stream(), providers.stream()).forEach(this::tryLoad);
  }

  private void tryLoad(Supplier<Addon> provider) {
    Addon addon = provider.get();
    try {
      addon.load(context);
      this.addons.add(addon);
    } catch (Throwable t) {
      logger().warn("Unable to load addon %s".formatted(addon.getClass().getName()), t);
    }
  }

  @Override
  public void enableAll(Game game) {
    forEachSafe(addon -> addon.enable(game));
  }

  @Override
  public void unloadAll() {
    forEachSafe(Addon::unload);
    addons.clear();
    try {
      loader.close();
    } catch (IOException e) {
      logger.warn(e.getMessage(), e);
    }
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

  @Override
  public Iterator<Addon> iterator() {
    return Collections.unmodifiableCollection(addons).iterator();
  }
}
