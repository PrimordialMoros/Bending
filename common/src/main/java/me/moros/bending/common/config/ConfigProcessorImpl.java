/*
 * Copyright 2020-2025 Moros
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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeModifier;
import me.moros.bending.api.config.attribute.AttributeValue;
import me.moros.bending.api.config.attribute.Modifier;
import me.moros.bending.api.user.AttributeUser;
import me.moros.bending.common.config.processor.CachedConfig;
import me.moros.bending.common.config.processor.CachedConfig.ConfigException;
import me.moros.bending.common.logging.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.reference.ConfigurationReference;

record ConfigProcessorImpl(Logger logger, ConfigurationReference<? extends ConfigurationNode> root,
                           Map<Class<? extends Configurable>, CachedConfig<?>> cache) implements ConfigProcessor {
  ConfigProcessorImpl(Logger logger, ConfigurationReference<? extends ConfigurationNode> root) {
    this(logger, root, new ConcurrentHashMap<>());
  }

  void cache(Class<? extends Configurable> configType) {
    getCachedConfig(configType);
  }

  @Override
  public <T extends Configurable> T calculate(AttributeUser user, AbilityDescription desc, Class<T> configType) {
    return getCachedConfig(configType)
      .withAttributes(collectActiveModifiers(user, desc), t -> logger.warn(t.getMessage(), t));
  }

  @Override
  public Collection<AttributeValue> listAttributes(AttributeUser user, AbilityDescription desc, Class<? extends Configurable> configType) {
    return getCachedConfig(configType).readAttributes(collectActiveModifiers(user, desc));
  }

  private Map<Attribute, Modifier> collectActiveModifiers(AttributeUser user, AbilityDescription desc) {
    return user.attributeModifiers().stream().filter(modifier -> modifier.policy().shouldModify(desc))
      .collect(Collectors.toMap(AttributeModifier::attribute, AttributeModifier::modifier, Modifier::merge));
  }

  @SuppressWarnings("unchecked")
  private <T extends Configurable> CachedConfig<T> getCachedConfig(Class<T> configType) {
    return (CachedConfig<T>) cache.computeIfAbsent(configType, s -> {
      try {
        return CachedConfig.createFrom(root, configType);
      } catch (ConfigException e) {
        logger.warn(e.getMessage(), e);
        return null;
      }
    });
  }
}
