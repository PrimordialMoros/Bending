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

package me.moros.bending.common.config.processor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeValue;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.config.attribute.Modifier;
import me.moros.bending.common.util.ReflectionUtil;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.NodePath;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.reference.ValueReference;
import org.spongepowered.configurate.serialize.SerializationException;

public final class CachedConfig<T extends Configurable> {
  private final Class<T> configType;
  private final T fallback;
  private final ValueReference<T, ? extends ConfigurationNode> configRef;

  private final Collection<Entry<Attribute, ConfigEntry>> entries;
  private final Map<Attribute, Collection<ConfigEntry>> map;

  private CachedConfig(Class<T> configType, T fallback, ValueReference<T, ? extends ConfigurationNode> configRef,
                       Collection<Entry<Attribute, ConfigEntry>> entries) {
    this.configType = configType;
    this.fallback = fallback;
    this.configRef = configRef;

    this.entries = List.copyOf(entries);
    this.map = new EnumMap<>(Attribute.class);
    for (var entry : this.entries) {
      this.map.computeIfAbsent(entry.getKey(), a -> new ArrayList<>()).add(entry.getValue());
    }
  }

  public T withAttributes(Map<Attribute, Modifier> activeModifiers, Consumer<Throwable> consumer) {
    if (activeModifiers.isEmpty()) {
      return configRef.get();
    }
    ConfigurationNode parentCopy = configRef.node().copy();
    for (var entry : activeModifiers.entrySet()) {
      Attribute attribute = entry.getKey();
      var modifier = entry.getValue();
      for (var handle : map.getOrDefault(attribute, List.of())) {
        handle.modify(parentCopy, modifier, consumer);
      }
    }
    try {
      return parentCopy.get(configType, fallback);
    } catch (SerializationException e) {
      return fallback;
    }
  }

  public Collection<AttributeValue> readAttributes(Map<Attribute, Modifier> activeModifiers) {
    Collection<AttributeValue> attributes = new ArrayList<>();
    ConfigurationNode parent = configRef.node();
    for (var entry : entries) {
      Attribute attribute = entry.getKey();
      var modifier = activeModifiers.get(attribute);
      attributes.add(entry.getValue().asAttributeValue(parent, attribute, modifier));
    }
    return attributes;
  }

  public static <T extends Configurable> CachedConfig<?> createFrom(ConfigurationReference<? extends ConfigurationNode> ref, Class<T> configType) throws ConfigException {
    T instance = ReflectionUtil.tryCreateInstance(configType);
    if (instance == null) {
      throw new ConfigException("Could not create %s instance.".formatted(configType.getName()));
    }
    try {
      var valueRef = ref.referenceTo(configType, NodePath.path(instance.path().toArray()), instance);
      List<Entry<Attribute, ConfigEntry>> handles = new ArrayList<>();
      for (Field field : configType.getDeclaredFields()) {
        Modifiable annotation = field.getAnnotation(Modifiable.class);
        if (annotation != null) {
          Attribute attribute = annotation.value();
          handles.add(Map.entry(attribute, new ConfigEntry(field)));
        }
      }
      return new CachedConfig<>(configType, instance, valueRef, handles);
    } catch (Exception e) {
      throw new ConfigException(e);
    }
  }

  public static final class ConfigException extends Exception {
    private ConfigException(Exception e) {
      super(e.getMessage(), e);
    }

    private ConfigException(String message) {
      super(message);
    }
  }
}
