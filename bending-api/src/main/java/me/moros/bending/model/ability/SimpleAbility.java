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

package me.moros.bending.model.ability;

import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.platform.block.Block;
import me.moros.bending.util.collision.CollisionUtil.CollisionCallback;

/**
 * Represents a simple ability.
 */
public interface SimpleAbility extends CollisionCallback {
  /**
   * Render the ability, called when ability is updated.
   */
  void render();

  /**
   * Handle ability after rendering is completed, typically used for playing sounds and extra calculations.
   */
  default void postRender() {
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
