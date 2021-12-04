/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import me.moros.bending.Bending;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeConverter;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.hocon.HoconConfigurationLoader;

public final class ConfigManager {
  private static final Map<Class<? extends Number>, AttributeConverter> converters = Map.of(
    Double.class, AttributeConverter.DOUBLE,
    Integer.class, AttributeConverter.INT,
    Long.class, AttributeConverter.LONG,
    double.class, AttributeConverter.DOUBLE,
    int.class, AttributeConverter.INT,
    long.class, AttributeConverter.LONG
  );

  private final Collection<Configurable> instances = new ArrayList<>();
  private final HoconConfigurationLoader loader;

  private CommentedConfigurationNode configRoot;

  public ConfigManager(@NonNull String directory) {
    Path path = Paths.get(directory, "bending.conf");
    loader = HoconConfigurationLoader.builder().path(path).build();
    try {
      Files.createDirectories(path.getParent());
      configRoot = loader.load();
    } catch (IOException e) {
      Bending.logger().warn(e.getMessage(), e);
    }
  }

  public void reload() {
    try {
      configRoot = loader.load();
      instances.forEach(Configurable::reload);
    } catch (IOException e) {
      Bending.logger().warn(e.getMessage(), e);
    }
  }

  public void save() {
    try {
      Bending.logger().info("Saving bending config");
      loader.save(configRoot);
    } catch (IOException e) {
      Bending.logger().warn(e.getMessage(), e);
    }
  }

  public @NonNull CommentedConfigurationNode config() {
    return configRoot;
  }

  public void add(@NonNull Configurable c) {
    instances.add(c);
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  public <T extends Configurable> T calculate(@NonNull Ability ability, @NonNull T config) {
    User user = ability.user();
    AbilityDescription desc = ability.description();
    Collection<AttributeModifier> activeModifiers = Registries.ATTRIBUTES.attributes(user)
      .filter(modifier -> modifier.policy().shouldModify(desc)).toList();

    if (activeModifiers.isEmpty()) {
      return config;
    }

    T newConfig;
    try {
      newConfig = (T) config.clone();
    } catch (CloneNotSupportedException e) {
      Bending.logger().warn(e.getMessage(), e);
      return config;
    }

    for (Field field : newConfig.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(Modifiable.class)) {
        boolean wasAccessible = field.isAccessible();
        field.setAccessible(true);
        modifyField(field, newConfig, activeModifiers);
        field.setAccessible(wasAccessible);
      }
    }

    return newConfig;
  }

  private void modifyField(Field field, Configurable config, Collection<AttributeModifier> activeModifiers) {
    double value;
    try {
      value = ((Number) field.get(config)).doubleValue();
    } catch (IllegalAccessException e) {
      Bending.logger().warn(e.getMessage(), e);
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
      field.set(config, converters.getOrDefault(field.getType(), AttributeConverter.DOUBLE).apply(value));
    } catch (IllegalAccessException e) {
      Bending.logger().warn(e.getMessage(), e);
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
