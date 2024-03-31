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
import me.moros.bending.common.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.serialize.SerializationException;

record ConfigProcessorImpl(Logger logger, ConfigurationReference<? extends ConfigurationNode> root,
                           Map<Class<? extends Configurable>, CachedConfig> cache) implements ConfigProcessor {
  ConfigProcessorImpl(Logger logger, ConfigurationReference<? extends ConfigurationNode> root) {
    this(logger, root, new ConcurrentHashMap<>());
  }

  <T extends Configurable> T get(T def) {
    return def.external() ? def : deserialize(root.node().node(def.path()), def);
  }

  @SuppressWarnings("unchecked")
  private <T extends Configurable> T deserialize(ConfigurationNode node, T def) {
    try {
      return (T) node.get(def.getClass(), def);
    } catch (SerializationException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public <T extends Configurable> T calculate(AttributeUser user, AbilityDescription desc, T config) {
    return modifyAttributes(collectActiveModifiers(user, desc), config);
  }

  @Override
  public Collection<AttributeValue> listAttributes(AttributeUser user, AbilityDescription desc, Configurable config) {
    return readAttributes(collectActiveModifiers(user, desc), config);
  }

  private Map<Attribute, ModificationMatrix> collectActiveModifiers(AttributeUser user, AbilityDescription desc) {
    return user.attributes().filter(modifier -> modifier.policy().shouldModify(desc))
      .collect(Collectors.toMap(AttributeModifier::attribute, ModificationMatrix::from, ModificationMatrix::merge));
  }

  private <T extends Configurable> T modifyAttributes(Map<Attribute, ModificationMatrix> activeModifiers, T config) {
    if (!activeModifiers.isEmpty()) {
      var cachedConfig = getCachedConfig(config);
      if (cachedConfig != null) {
        if (config.external()) {
          return modifyAttributes(cachedConfig, activeModifiers, config);
        } else {
          return deserialize(modifyAttributes(cachedConfig, activeModifiers, nodeFor(config).copy()), config);
        }
      }
    }
    return config;
  }

  private <T> T modifyAttributes(CachedConfig cachedConfig, Map<Attribute, ModificationMatrix> activeModifiers, T parent) {
    for (var entry : activeModifiers.entrySet()) {
      Attribute attribute = entry.getKey();
      var modifier = entry.getValue();
      for (var handle : cachedConfig.getKeysFor(attribute)) {
        handle.modify(parent, modifier, t -> logger.warn(t.getMessage(), t));
      }
    }
    return parent;
  }

  private Collection<AttributeValue> readAttributes(Map<Attribute, ModificationMatrix> activeModifiers, Configurable config) {
    var cachedConfig = getCachedConfig(config);
    if (cachedConfig == null) {
      return List.of();
    }
    Collection<AttributeValue> attributes = new ArrayList<>();
    Object parent = config.external() ? config : nodeFor(config);
    for (var entry : cachedConfig) {
      Attribute attribute = entry.getKey();
      var modifier = activeModifiers.get(attribute);
      attributes.add(entry.getValue().asAttributeValue(parent, attribute, modifier));
    }
    return attributes;
  }

  private ConfigurationNode nodeFor(Configurable instance) {
    if (instance.external()) {
      throw new IllegalArgumentException("No node for external config!");
    }
    return root.node().node(instance.path());
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
