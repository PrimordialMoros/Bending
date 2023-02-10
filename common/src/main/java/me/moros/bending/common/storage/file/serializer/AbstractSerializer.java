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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

public abstract class AbstractSerializer<T> implements TypeSerializer<T> {
  public static @Nullable ConfigurationNode nonVirtualNodeOrNull(ConfigurationNode source, String path) {
    return source.hasChild(path) ? source.node(path) : null;
  }

  public static ConfigurationNode nonVirtualNode(ConfigurationNode source, String path) throws SerializationException {
    var node = nonVirtualNodeOrNull(source, path);
    if (node == null) {
      throw new SerializationException("Required field " + path + " was not present in node");
    }
    return node;
  }

  public static <E, R> List<@Nullable R> mapToList(Map<Integer, E> input, Function<E, @Nullable R> mapper, int maxIndex) {
    List<R> output = new ArrayList<>(maxIndex);
    for (int i = 0; i < maxIndex; i++) {
      var current = input.get(i + 1);
      output.add(current == null ? null : mapper.apply(current));
    }
    return output;
  }

  public static <E, R> Map<Integer, R> listToMap(List<@Nullable E> input, Function<@NonNull E, R> mapper, int maxIndex) {
    Map<Integer, R> output = new LinkedHashMap<>(maxIndex);
    for (int i = 0; i < Math.min(input.size(), maxIndex); i++) {
      var current = input.get(i);
      if (current != null) {
        output.put(i + 1, mapper.apply(current));
      }
    }
    return output;
  }
}
