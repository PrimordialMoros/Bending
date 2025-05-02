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

package me.moros.bending.api.ability;

import me.moros.bending.api.collision.CollisionUtil.CollisionCallback;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.platform.block.Block;
import me.moros.math.Vector3d;

/**
 * Represents a simple ability.
 */
public interface SimpleAbility extends CollisionCallback {
  /**
   * Render the ability, called when ability is updated.
   */
  @Deprecated(forRemoval = true, since = "3.12.0")
  default void render() {
    render(collider().position());
  }

  /**
   * Handle ability after rendering is completed, typically used for playing sounds and extra calculations.
   */
  @Deprecated(forRemoval = true, since = "3.12.0")
  default void postRender() {
    postRender(collider().position());
  }

  /**
   * Render the ability, called when ability is updated.
   * @param location the location to render at
   */
  void render(Vector3d location);

  /**
   * Handle ability after rendering is completed, typically used for playing sounds and extra calculations.
   * @param location the location the ability was just rendered at
   */
  default void postRender(Vector3d location) {
  }

  /**
   * Called when a collision with a block has been detected.
   * @param block the block that collided.
   * @return true if the block was hit, false otherwise
   */
  boolean onBlockHit(Block block);

  /**
   * Get the collider for this ability.
   * @return this instance's collider
   */
  Collider collider();
}
