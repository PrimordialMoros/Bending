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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.DoubleFunction;

import com.google.common.collect.Iterables;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.config.ConfigProcessor;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.AttributeModifier;
import me.moros.bending.api.config.attribute.AttributeValue;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.user.AttributeUser;
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
  public <T extends Configurable> T calculate(AttributeUser user, AbilityDescription desc, T config) {
    return process(user, desc, get(config));
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

  private <T extends Configurable> T process(AttributeUser user, AbilityDescription desc, T copy) {
    Collection<AttributeModifier> activeModifiers = collectActiveModifiers(user, desc);
    if (!activeModifiers.isEmpty()) {
      forEachAttribute(activeModifiers, copy, (f, a, b, m) -> f.set(copy, m));
    }
    return copy;
  }

  @Override
  public Collection<AttributeValue> listAttributes(AttributeUser user, AbilityDescription desc, Configurable config) {
    Collection<AttributeModifier> activeModifiers = collectActiveModifiers(user, desc);
    Collection<AttributeValue> attributes = new ArrayList<>();
    forEachAttribute(activeModifiers, config, (f, a, b, m) -> attributes.add(AttributeValue.of(a, f.getName(), b, m)));
    return attributes;
  }

  private Collection<AttributeModifier> collectActiveModifiers(AttributeUser user, AbilityDescription desc) {
    return user.attributes().filter(modifier -> modifier.policy().shouldModify(desc)).toList();
  }

  private void forEachAttribute(Collection<AttributeModifier> activeModifiers, Configurable instance, MultiConsumer consumer) {
    for (Field field : instance.getClass().getDeclaredFields()) {
      Modifiable annotation = field.getAnnotation(Modifiable.class);
      if (annotation != null) {
        Attribute attribute = annotation.value();
        boolean wasAccessible = field.canAccess(instance);
        try {
          field.setAccessible(true);
          Number baseValue = ((Number) field.get(instance));
          Number finalValue = baseValue;
          if (!activeModifiers.isEmpty()) {
            double base = baseValue.doubleValue();
            double modified = modifyAttribute(base, Iterables.filter(activeModifiers, a -> a.attribute() == attribute));
            if (base != modified) {
              finalValue = CONVERTERS.getOrDefault(field.getType(), x -> x).apply(modified);
            }
          }
          consumer.accept(field, attribute, baseValue, finalValue);
        } catch (IllegalAccessException e) {
          logger.warn(e.getMessage(), e);
        } finally {
          field.setAccessible(wasAccessible);
        }
      }
    }
  }

  private double modifyAttribute(double baseValue, Iterable<AttributeModifier> filteredModifiers) {
    double[] operations = new double[]{0, 1, 1};
    for (AttributeModifier modifier : filteredModifiers) {
      switch (modifier.type()) {
        case ADDITIVE -> operations[0] += modifier.value();
        case SUMMED_MULTIPLICATIVE -> operations[1] += modifier.value();
        case MULTIPLICATIVE -> operations[2] *= modifier.value();
      }
    }
    return (baseValue + operations[0]) * operations[1] * operations[2];
  }

  @FunctionalInterface
  private interface MultiConsumer {
    void accept(Field field, Attribute attribute, Number baseValue, Number finalValue) throws IllegalAccessException;
  }
}
