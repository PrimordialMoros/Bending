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

package me.moros.bending.model.protection;

import me.moros.bending.model.key.Keyed;
import me.moros.bending.registry.Registries;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

/**
 * Interface that models a region/block protection plugin.
 * Protections can be registered in {@link Registries#PROTECTIONS}.
 */
public interface Protection extends Keyed {
  String NAMESPACE = "bending.protection";

  /**
   * Test if a user can build at the specified block location.
   * @param entity the entity to check
   * @param block the block to check
   * @return the result
   */
  boolean canBuild(LivingEntity entity, Block block);
}
