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

package me.moros.codegen.vanilla;

import java.io.File;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import me.moros.codegen.Generators;
import net.minecraft.resources.ResourceLocation;

record RegistryTagGenerator(Path directory) implements Generator {
  @Override
  public Collection<ResourceLocation> generate() {
    try (var stream = Files.walk(directory)) {
      return stream.filter(Files::isRegularFile).map(this::toResourceLocation).toList();
    } catch (IOException e) {
      Generators.LOGGER.log(Level.ERROR, e.getMessage(), e);
    }
    return List.of();
  }

  private ResourceLocation toResourceLocation(Path path) {
    final String fileName = directory.relativize(path).toString();
    final String name = (File.separatorChar == '\\' ? fileName.replace('\\', '/') : fileName)
      .replace(".json", "");
    return new ResourceLocation(name);
  }
}
