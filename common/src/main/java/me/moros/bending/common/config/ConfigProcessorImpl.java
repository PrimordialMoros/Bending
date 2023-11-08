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

package me.moros.bending.common.config;

import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.function.DoubleFunction;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeModifier;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.config.attribute.ModifierOperation;
import me.moros.bending.api.user.User;
import me.moros.bending.common.logging.Logger;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.reference.ConfigurationReference;
import org.spongepowered.configurate.serialize.SerializationException;

@SuppressWarnings("unchecked")
record ConfigProcessorImpl(Logger logger,
                           ConfigurationReference<CommentedConfigurationNode> root) implements ConfigProcessor {
  private static final Map<Class<? extends Number>, DoubleFunction<Number>> CONVERTERS = Map.of(
    Double.class, x -> x,
    Integer.class, x -> (int) x,
    Long.class, x -> (long) x,
    double.class, x -> x,
    int.class, x -> (int) x,
    long.class, x -> (long) x
  );

  @Override
  public <T extends Configurable> T calculate(Ability ability, T config) {
    return process(ability, get(config));
  }

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
      field.set(config, CONVERTERS.getOrDefault(field.getType(), x -> x).apply(value));
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
