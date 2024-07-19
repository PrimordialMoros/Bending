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

package me.moros.bending.api.config;

import java.util.Collection;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.config.attribute.AttributeModifier;
import me.moros.bending.api.config.attribute.AttributeValue;
import me.moros.bending.api.user.AttributeUser;

/**
 * Processes {@link Configurable}s by applying attribute modifiers.
 */
public interface ConfigProcessor {
  /**
   * Calculates new values for the given config after applying {@link AttributeModifier}s.
   * <p>Note: By default, this method will return a copy of the supplied object, that is loaded from the
   * main configuration file. For abilities with external configs, they must override
   * {@link Configurable#external()} to return true. In that case, the method will operate on the same object
   * that is supplied, so you should make sure to always pass a fresh copy yourself.
   * @param ability the ability the config belongs to
   * @param configType the type of config to process
   * @param <T> the type of config
   * @return the modified config
   */
  default <T extends Configurable> T calculate(Ability ability, Class<T> configType) {
    return calculate(ability.user(), ability.description(), configType);
  }

  /**
   * Calculates new values for the given config after applying {@link AttributeModifier}s.
   * <p>Note: By default, this method will return a copy of the supplied object, that is loaded from the
   * main configuration file. For abilities with external configs, they must override
   * {@link Configurable#external()} to return true. In that case, the method will operate on the same object
   * that is supplied, so you should make sure to always pass a fresh copy yourself.
   * @param user the attribute user
   * @param desc the ability description
   * @param configType the type of config to process
   * @param <T> the type of config
   * @return the modified config
   */
  <T extends Configurable> T calculate(AttributeUser user, AbilityDescription desc, Class<T> configType);

  /**
   * Utility method that calculates all attributes in a given config.
   * @param user the attribute user
   * @param desc the ability description
   * @param configType the type of config to process
   * @return a collection of all attributes found
   */
  Collection<AttributeValue> listAttributes(AttributeUser user, AbilityDescription desc, Class<? extends Configurable> configType);
}
