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

package me.moros.bending.config;

import java.io.Serializable;

import me.moros.bending.model.ability.Ability;
import org.spongepowered.configurate.CommentedConfigurationNode;

/**
 * This is an abstract class that defines a serializable config.
 */
public abstract class Configurable implements Serializable {
  private transient CommentedConfigurationNode node;

  protected Configurable() {
  }

  /**
   * Provides the path that serves as the root node for this configuration when serialized.
   * @return the path of this configuration's root node.
   */
  public abstract Iterable<String> path();

  /**
   * Controls if this configuration external and cannot be loaded from the main configuration file.
   * @return whether this configuration is external
   * @see ConfigProcessor#calculate(Ability, Configurable)
   */
  public boolean external() {
    return false;
  }

  CommentedConfigurationNode node() {
    return node;
  }

  void setNode(CommentedConfigurationNode node) {
    this.node = node;
  }
}
