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

package me.moros.bending.game;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.AttributeConverter;
import me.moros.bending.model.attribute.AttributeModifier;
import me.moros.bending.model.attribute.ModifierOperation;
import me.moros.bending.model.attribute.ModifyPolicy;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

// TODO Expand system to include rounding, range checking etc and profile performance
public final class AttributeSystem {
  private static final Map<Class<? extends Number>, AttributeConverter> converters = new HashMap<>(); // Converts a double into some other numeric type
  private final Map<User, Collection<UserModifier>> modifierMap = new HashMap<>();

  static {
    converters.put(Double.class, AttributeConverter.DOUBLE);
    converters.put(Integer.class, AttributeConverter.INT);
    converters.put(Long.class, AttributeConverter.LONG);
    converters.put(double.class, AttributeConverter.DOUBLE);
    converters.put(int.class, AttributeConverter.INT);
    converters.put(long.class, AttributeConverter.LONG);
  }

  // Add a modifier that's only active according to some policy.
  public @NonNull UserModifier addModifier(@NonNull User user, @NonNull AttributeModifier modifier, @NonNull ModifyPolicy policy) {
    Collection<UserModifier> modifiers = modifierMap.computeIfAbsent(user, key -> new ArrayList<>());
    UserModifier userModifier = new UserModifier(modifier, policy);
    modifiers.add(userModifier);
    return userModifier;
  }

  public void clearModifiers(@NonNull User user) {
    modifierMap.remove(user);
  }

  public boolean removeModifier(@NonNull User user, @NonNull AttributeModifier modifier, @NonNull ModifyPolicy policy) {
    return removeModifier(user, new UserModifier(modifier, policy));
  }

  public boolean removeModifier(@NonNull User user, @NonNull UserModifier modifier) {
    Collection<UserModifier> modifiers = modifierMap.get(user);
    if (modifiers == null) {
      return false;
    }
    boolean result = modifiers.remove(modifier);
    if (modifiers.isEmpty()) {
      modifierMap.remove(user);
    }
    return result;
  }

  // Recalculates all of the config values for the user's instances.
  public void recalculate(@NonNull User user) {
    Bending.game().abilityManager(user.world()).userInstances(user).forEach(Ability::recalculateConfig);
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  public <T extends Configurable> T calculate(@NonNull Ability ability, @NonNull T config) {
    User user = ability.user();
    if (!modifierMap.containsKey(user)) {
      return config;
    }
    Collection<UserModifier> activeModifiers = modifierMap.get(user).stream()
      .filter(modifier -> modifier.policy.shouldModify(ability))
      .collect(Collectors.toList());

    T newConfig;
    try {
      newConfig = (T) config.clone();
    } catch (CloneNotSupportedException e) {
      e.printStackTrace();
      return config;
    }

    for (Field field : newConfig.getClass().getDeclaredFields()) {
      if (field.isAnnotationPresent(Attribute.class)) {
        boolean wasAccessible = field.isAccessible();
        field.setAccessible(true);
        modifyField(field, newConfig, activeModifiers);
        field.setAccessible(wasAccessible);
      }
    }

    return newConfig;
  }

  private boolean modifyField(Field field, Configurable config, Collection<UserModifier> userModifiers) {
    double value;
    try {
      value = ((Number) field.get(config)).doubleValue();
    } catch (IllegalAccessException e) {
      Bending.logger().warn(e.getMessage());
      return false;
    }

    double addOperation = 0.0;
    double multiplyOperation = 1.0;
    Collection<Double> multiplicativeOperations = new ArrayList<>();
    for (UserModifier userModifier : userModifiers) {
      AttributeModifier modifier = userModifier.modifier;
      if (hasAttribute(field, modifier.attribute())) {
        if (modifier.type() == ModifierOperation.ADDITIVE) {
          addOperation += modifier.value();
        } else if (modifier.type() == ModifierOperation.SUMMED_MULTIPLICATIVE) {
          multiplyOperation += modifier.value();
        } else if (modifier.type() == ModifierOperation.MULTIPLICATIVE) {
          multiplicativeOperations.add(modifier.value());
        }
      }
    }
    value = (value + addOperation) * multiplyOperation;
    for (double amount : multiplicativeOperations) {
      value *= amount;
    }
    try {
      field.set(config, converters.getOrDefault(field.getType(), AttributeConverter.DOUBLE).apply(value));
    } catch (IllegalAccessException e) {
      Bending.logger().warn(e.getMessage());
      return false;
    }

    return true;
  }

  private boolean hasAttribute(Field field, String attributeName) {
    for (Attribute attribute : field.getAnnotationsByType(Attribute.class)) {
      if (attribute.value().equals(attributeName)) {
        return true;
      }
    }
    return false;
  }

  private static class UserModifier {
    final AttributeModifier modifier;
    final ModifyPolicy policy;

    UserModifier(AttributeModifier modifier, ModifyPolicy policy) {
      this.modifier = modifier;
      this.policy = policy;
    }
  }

  public static @NonNull ModifyPolicy elementPolicy(@NonNull Element element) {
    return ability -> ability.description().element() == element;
  }

  public static @NonNull ModifyPolicy abilityPolicy(@NonNull AbilityDescription desc) {
    return ability -> ability.description().equals(desc);
  }
}
