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

package me.moros.bending.common.config.processor;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import org.spongepowered.configurate.util.NamingSchemes;

public final class CachedConfig implements Iterable<Entry<Attribute, ConfigEntry>> {
  private final Collection<Entry<Attribute, ConfigEntry>> entries;
  private final Map<Attribute, Collection<ConfigEntry>> map;

  private CachedConfig(Collection<Entry<Attribute, ConfigEntry>> entries) {
    this.entries = List.copyOf(entries);
    this.map = new EnumMap<>(Attribute.class);
    for (var entry : this.entries) {
      this.map.computeIfAbsent(entry.getKey(), a -> new ArrayList<>()).add(entry.getValue());
    }
  }

  public Iterable<ConfigEntry> getKeysFor(Attribute attribute) {
    return map.getOrDefault(attribute, List.of());
  }

  @Override
  public Iterator<Entry<Attribute, ConfigEntry>> iterator() {
    return entries.iterator();
  }

  public static CachedConfig createFrom(Configurable instance) throws IllegalAccessException {
    ConfigEntryFactory factory = instance.external() ? varHandleFactory(instance.getClass()) : nodeFactory();
    List<Entry<Attribute, ConfigEntry>> handles = new ArrayList<>();
    for (Field field : instance.getClass().getDeclaredFields()) {
      Modifiable annotation = field.getAnnotation(Modifiable.class);
      if (annotation != null) {
        Attribute attribute = annotation.value();
        handles.add(Map.entry(attribute, factory.create(field)));
      }
    }
    return new CachedConfig(handles);
  }

  private static ConfigEntryFactory varHandleFactory(Class<? extends Configurable> configClass) throws IllegalAccessException {
    Lookup lookup = MethodHandles.privateLookupIn(configClass, MethodHandles.lookup());
    return field -> ConfigEntry.fromVarHandle(field.getName(), field.getType(), lookup.unreflectVarHandle(field));
  }

  private static ConfigEntryFactory nodeFactory() {
    return field -> ConfigEntry.fromNode(NamingSchemes.LOWER_CASE_DASHED.coerce(field.getName()), field.getType());
  }

  @FunctionalInterface
  private interface ConfigEntryFactory {
    ConfigEntry create(Field field) throws IllegalAccessException;
  }
}
