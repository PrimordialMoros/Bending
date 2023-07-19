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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.ServiceLoader.Provider;
import java.util.stream.Collectors;

import me.moros.bending.api.addon.Addon;
import me.moros.bending.common.logging.Logger;

record AddonLoaderImpl(Logger logger, AddonClassLoader loader, Collection<Addon> addons) implements AddonLoader {
  AddonLoaderImpl(Logger logger, AddonClassLoader loader, Collection<Addon> addons) {
    this.logger = logger;
    this.loader = loader;
    this.addons = new HashSet<>(addons);
    this.addons.addAll(ServiceLoader.load(Addon.class, loader).stream().map(Provider::get).collect(Collectors.toSet()));
  }

  @Override
  public Iterator<Addon> iterator() {
    return Collections.unmodifiableCollection(addons).iterator();
  }
}
