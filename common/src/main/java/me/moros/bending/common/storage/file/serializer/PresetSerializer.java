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

package me.moros.bending.common.storage.file.serializer;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.leangen.geantyref.TypeToken;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.util.TextUtil;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

final class PresetSerializer extends AbstractSerializer<Preset> {
  static final PresetSerializer INSTANCE = new PresetSerializer();

  private static final TypeToken<Map<Integer, String>> MAP_TOKEN = new TypeToken<>() {
  };

  private PresetSerializer() {
  }

  @Override
  public Preset deserialize(Type type, ConfigurationNode source) throws SerializationException {
    String name = Optional.ofNullable(source.key()).map(Object::toString).orElse("");
    if (!TextUtil.sanitizeInput(name).equals(name)) {
      throw new SerializationException(source, String.class, "Invalid preset name: " + name);
    }
    Map<Integer, String> input = source.get(MAP_TOKEN, Map.of());
    AbilityDescription[] output = new AbilityDescription[9];
    if (!input.isEmpty()) {
      for (int i = 0; i < output.length; i++) {
        var current = input.get(i + 1);
        if (current != null) {
          output[i] = Registries.ABILITIES.fromString(current);
        }
      }
    }
    Preset result = Preset.create(name, output);
    if (result.isEmpty()) {
      throw new SerializationException("Empty preset " + name);
    }
    return result;
  }

  @Override
  public void serialize(Type type, @Nullable Preset preset, ConfigurationNode target) throws SerializationException {
    if (preset == null || preset.isEmpty()) {
      target.raw(null);
      return;
    }
    Map<Integer, String> output = new LinkedHashMap<>(12); // 9 / 0.75 (load factor)
    preset.forEach((desc, idx) -> output.put(idx + 1, desc.key().asString()));
    target.set(MAP_TOKEN, output);
  }
}
