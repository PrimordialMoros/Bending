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

package me.moros.bending.common.storage.file.loader;

import java.nio.file.Path;

import me.moros.bending.common.storage.file.serializer.Serializers;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.AbstractConfigurationLoader;
import org.spongepowered.configurate.reference.ConfigurationReference;

@FunctionalInterface
public interface Loader<B extends AbstractConfigurationLoader.Builder<B, ?>> {
  B loaderBuilder();

  default B withSerializers() {
    return loaderBuilder().defaultOptions(o -> o.serializers(b -> b.registerAll(Serializers.ALL)));
  }

  default ConfigurationReference<? extends ConfigurationNode> load(Path path) throws ConfigurateException {
    return withSerializers().path(path).build().loadToReference();
  }
}
