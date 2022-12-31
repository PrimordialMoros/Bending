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

package me.moros.bending.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.CompletionException;

import me.moros.bending.util.Tasker;
import net.kyori.adventure.key.Key;
import org.slf4j.Logger;

public abstract class Initializer {
  private final Path path;
  private final Logger logger;

  protected Initializer(String path, Logger logger) {
    this.path = Path.of(path);
    this.logger = logger;
    init();
  }

  protected void checkMissing(String logFile, String msg, Collection<Key> missing) {
    if (!missing.isEmpty()) {
      logger.warn(String.format(msg, missing.size()));
      var lines = missing.stream().map(Key::asString).toList();
      Tasker.async().submit(() -> {
        try {
          Files.write(path.resolve(logFile), lines);
        } catch (IOException e) {
          throw new CompletionException(e);
        }
      });
    }
  }

  protected abstract void init();
}
