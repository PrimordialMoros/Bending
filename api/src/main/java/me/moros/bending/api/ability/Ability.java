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

package me.moros.bending.api.ability;

import java.util.Collection;
import java.util.List;

import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.game.AbilityManager;
import me.moros.bending.api.user.User;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Represents an ability that can be instantiated.
 */
public interface Ability extends Updatable {
  /**
   * Attempt to initialize and activate this ability instance.
   * @param user the user that controls this ability
   * @param method the type of activation that is used
   * @return true if ability was successfully activated, false otherwise
   */
  boolean activate(User user, Activation method);

  /**
   * Load the config and apply any possible modifiers.
   */
  void loadConfig();

  /**
   * Get the ability description associated with this ability instance.
   * @return the type of this ability
   */
  AbilityDescription description();

  /**
   * Get the user that is currently controlling this ability instance.
   * @return the user for this ability
   */
  @MonotonicNonNull User user();

  /**
   * Called when the user of the ability instance is changed.
   * @param newUser the new user that controls this ability
   * @see AbilityManager#changeOwner(Ability, User)
   */
  default void onUserChange(User newUser) {
  }

  /**
   * Get the colliders for this ability instance.
   * @return an immutable collection of this ability's colliders
   */
  default Collection<Collider> colliders() {
    return List.of();
  }

  /**
   * Called when a collision with another ability instance occurs.
   * @param collision the data regarding the collision that occurred.
   */
  default void onCollision(Collision collision) {
  }

  /**
   * Called when the ability is removed.
   * @see AbilityManager#destroyInstance(Ability)
   */
  default void onDestroy() {
  }
}
