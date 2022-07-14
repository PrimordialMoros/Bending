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

package me.moros.bending.model.ability;

import java.util.Collection;
import java.util.List;

import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.manager.AbilityManager;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public interface Ability extends Updatable {
  /**
   * Attempt to initialize and activate this ability.
   * @param user the user that controls the ability
   * @param method the type of activation that is used
   * @return true if ability was successfully activated, false otherwise
   */
  boolean activate(User user, Activation method);

  /**
   * Load the config and apply any possible modifiers.
   */
  void loadConfig();

  /**
   * @return the type of this ability
   */
  AbilityDescription description();

  /**
   * @return the user that is currently controlling the ability
   */
  @MonotonicNonNull User user();

  /**
   * Called when the user of the ability instance is changed.
   * @param newUser the new user that controls the ability
   * @see AbilityManager#changeOwner(Ability, User)
   */
  default void onUserChange(User newUser) {
  }

  /**
   * @return an immutable collection of this ability's colliders
   */
  default Collection<Collider> colliders() {
    return List.of();
  }

  /**
   * Called when a collision with another ability occurs.
   * @param collision the data regarding the collision that occured.
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
