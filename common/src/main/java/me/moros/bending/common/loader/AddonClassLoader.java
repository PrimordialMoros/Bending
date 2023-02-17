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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

final class AddonClassLoader extends URLClassLoader {
  static {
    ClassLoader.registerAsParallelCapable();
  }

  AddonClassLoader(ClassLoader parent) {
    super(new URL[]{}, parent);
  }

  AddonClassLoader loadJars(Path dir) {
    try {
      Files.createDirectories(dir);
      try (Stream<Path> stream = Files.list(dir)) {
        stream.filter(f -> f.getFileName().toString().endsWith(".jar")).forEach(this::tryAddUrl);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return this;
  }

  private void tryAddUrl(Path path) {
    try {
      addURL(path.toUri().toURL());
    } catch (MalformedURLException ignore) {
    }
  }
}
