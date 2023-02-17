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

package me.moros.bending.api.collision.raytrace;

import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.math.Vector3d;

public interface CompositeRayTrace extends EntityRayTrace, BlockRayTrace {
  /**
   * @deprecated use {@link RayTrace#miss(Vector3d)}
   */
  @Deprecated
  static CompositeRayTrace miss(Vector3d position) {
    return RayTrace.miss(position);
  }

  /**
   * @deprecated use {@link RayTrace#hit(Vector3d, Block)}
   */
  @Deprecated
  static CompositeRayTrace hit(Vector3d position, Block block) {
    return RayTrace.hit(position, block);
  }

  /**
   * @deprecated use {@link RayTrace#hit(Vector3d, Entity)}
   */
  @Deprecated
  static CompositeRayTrace hit(Vector3d position, Entity entity) {
    return RayTrace.hit(position, entity);
  }
}
