/*
 * Copyright 2020-2022 Moros
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

package me.moros.bending.config;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeConverter;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.user.User;
import org.slf4j.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.serialize.SerializationException;

public final class ConfigProcessor {
  private static final Map<Class<? extends Number>, AttributeConverter> CONVERTERS = Map.of(
    Double.class, AttributeConverter.DOUBLE,
    Integer.class, AttributeConverter.INT,
    Long.class, AttributeConverter.LONG,
    double.class, AttributeConverter.DOUBLE,
    int.class, AttributeConverter.INT,
    long.class, AttributeConverter.LONG
  );

  private final Logger logger;
  private final ConfigurationReference<CommentedConfigurationNode> root;

  ConfigProcessor(Logger logger, ConfigurationReference<CommentedConfigurationNode> root) {
    this.logger = logger;
    this.root = root;
  }

  public <T extends Configurable> T calculate(Ability ability, T config) {
    T copied = config.external() ? config : get(config);
    return process(ability, copied);
  }

  @SuppressWarnings("unchecked")
  <T extends Configurable> T get(T def) {
    CommentedConfigurationNode node = root.node().node(def.path());
    def.setNode(node);
    try {
      T result = (T) node.get(def.getClass(), def);
      result.setNode(node);
      return result;
    } catch (SerializationException e) {
      throw new RuntimeException(e);
    }
  }

  private <T extends Configurable> T process(Ability ability, T copy) {
    User user = ability.user();
    AbilityDescription desc = ability.description();
    Collection<AttributeModifier> activeModifiers = user.attributes()
      .filter(modifier -> modifier.policy().shouldModify(desc)).toList();
    if (activeModifiers.isEmpty()) {
      return copy;
    }
    for (Field field : copy.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(Modifiable.class)) {
        boolean wasAccessible = field.canAccess(copy);
        field.setAccessible(true);
        modifyField(field, copy, activeModifiers);
        field.setAccessible(wasAccessible);
      }
    }
    return copy;
  }

  private void modifyField(Field field, Object config, Iterable<AttributeModifier> activeModifiers) {
    double value;
    try {
      value = ((Number) field.get(config)).doubleValue();
    } catch (IllegalAccessException e) {
      logger.warn(e.getMessage(), e);
      return;
    }

    double[] operations = new double[]{0, 1, 1};
    for (AttributeModifier modifier : activeModifiers) {
      if (hasAttribute(field, modifier.attribute())) {
        if (modifier.type() == ModifierOperation.ADDITIVE) {
          operations[0] += modifier.value();
        } else if (modifier.type() == ModifierOperation.SUMMED_MULTIPLICATIVE) {
          operations[1] += modifier.value();
        } else if (modifier.type() == ModifierOperation.MULTIPLICATIVE) {
          operations[2] *= modifier.value();
        }
      }
    }
    value = (value + operations[0]) * operations[1] * operations[2];
    try {
      field.set(config, CONVERTERS.getOrDefault(field.getType(), AttributeConverter.DOUBLE).apply(value));
    } catch (IllegalAccessException e) {
      logger.warn(e.getMessage(), e);
    }
  }

  private boolean hasAttribute(Field field, Attribute attribute) {
    for (Modifiable a : field.getAnnotationsByType(Modifiable.class)) {
      if (attribute.equals(a.value())) {
        return true;
      }
    }
    return false;
  }
}
