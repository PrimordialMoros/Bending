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

package me.moros.bending.common.storage.file.serializer;

import java.lang.reflect.Type;
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

  private final TypeToken<Map<Integer, String>> mapToken = new TypeToken<>() {
  };

  private PresetSerializer() {
  }

  @Override
  public Preset deserialize(Type type, ConfigurationNode source) throws SerializationException {
    String name = Optional.ofNullable(source.key()).map(Object::toString).orElse("");
    if (name.isEmpty() || !TextUtil.sanitizeInput(name).equals(name)) {
      throw new SerializationException(source, String.class, "Invalid preset name: " + name);
    }
    var slots = mapToList(source.get(mapToken, Map.of()), Registries.ABILITIES::fromString, 9);
    if (slots.isEmpty()) {
      throw new SerializationException("Empty preset " + name);
    }
    return Preset.create(name.hashCode() & 0x7FFFFFFF, name, slots.toArray(AbilityDescription[]::new));
  }

  @Override
  public void serialize(Type type, @Nullable Preset preset, ConfigurationNode target) throws SerializationException {
    if (preset == null) {
      target.raw(null);
      return;
    }
    target.set(mapToken, listToMap(preset.abilities(), AbilityDescription::name, 9));
  }
}
