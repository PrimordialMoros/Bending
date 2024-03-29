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

package me.moros.bending.common.config;

import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeModifier;
import me.moros.bending.api.config.attribute.AttributeValue;
import me.moros.bending.api.user.AttributeUser;
import me.moros.bending.common.config.processor.CachedConfig;
import me.moros.bending.common.config.processor.ModificationMatrix;
import me.moros.bending.common.config.processor.NamedVarHandle;
import me.moros.bending.common.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.serialize.SerializationException;

record ConfigProcessorImpl(Logger logger, ConfigurationReference<CommentedConfigurationNode> root,
                           Map<Class<? extends Configurable>, CachedConfig> cache) implements ConfigProcessor {
  ConfigProcessorImpl(Logger logger, ConfigurationReference<CommentedConfigurationNode> root) {
    this(logger, root, new ConcurrentHashMap<>());
  }

  @Override
  public <T extends Configurable> T calculate(AttributeUser user, AbilityDescription desc, T config) {
    return process(user, desc, get(config));
  }

  @SuppressWarnings("unchecked")
  <T extends Configurable> T get(T def) {
    if (def.external()) {
      return def;
    }
    CommentedConfigurationNode node = root.node().node(def.path());
    try {
      return (T) node.get(def.getClass(), def);
    } catch (SerializationException e) {
      throw new UncheckedIOException(e);
    }
  }

  private <T extends Configurable> T process(AttributeUser user, AbilityDescription desc, T copy) {
    modifyAttributes(copy, collectActiveModifiers(user, desc));
    return copy;
  }

  @Override
  public Collection<AttributeValue> listAttributes(AttributeUser user, AbilityDescription desc, Configurable config) {
    return readAttributes(get(config), collectActiveModifiers(user, desc));
  }

  private Map<Attribute, ModificationMatrix> collectActiveModifiers(AttributeUser user, AbilityDescription desc) {
    return user.attributes().filter(modifier -> modifier.policy().shouldModify(desc))
      .collect(Collectors.toMap(AttributeModifier::attribute, ModificationMatrix::from, ModificationMatrix::merge));
  }

  private void modifyAttributes(Configurable instance, Map<Attribute, ModificationMatrix> activeModifiers) {
    if (activeModifiers.isEmpty()) {
      return;
    }
    var cachedConfig = getCachedConfig(instance);
    if (cachedConfig != null) {
      for (var entry : activeModifiers.entrySet()) {
        Attribute attribute = entry.getKey();
        ModificationMatrix matrix = entry.getValue();
        for (var handle : cachedConfig.getVarHandlesFor(attribute)) {
          handle.modify(instance, matrix);
        }
      }
    }
  }

  private Collection<AttributeValue> readAttributes(Configurable instance, Map<Attribute, ModificationMatrix> activeModifiers) {
    var cachedConfig = getCachedConfig(instance);
    if (cachedConfig == null) {
      return List.of();
    }
    Collection<AttributeValue> attributes = new ArrayList<>();
    for (var entry : cachedConfig) {
      Attribute attribute = entry.getKey();
      ModificationMatrix matrix = activeModifiers.get(attribute);
      NamedVarHandle handle = entry.getValue();
      attributes.add(handle.asAttributeValue(instance, attribute, matrix));
    }
    return attributes;
  }

  private @Nullable CachedConfig getCachedConfig(Configurable instance) {
    return cache.computeIfAbsent(instance.getClass(), s -> {
      try {
        return CachedConfig.createFrom(instance);
      } catch (IllegalAccessException e) {
        logger.warn(e.getMessage(), e);
        return null;
      }
    });
  }
}
