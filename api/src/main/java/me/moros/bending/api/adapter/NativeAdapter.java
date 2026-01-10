/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.api.adapter;

import me.moros.bending.api.event.BendingDamageEvent;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Interface for all NMS and Packet shenanigans that Bending takes advantage of.
 */
@Internal
public interface NativeAdapter extends PacketUtil {
  default boolean damage(BendingDamageEvent event) {
    return event.target().damage(event.damage(), event.user());
  }

  /**
   * Attempt to use NMS to set BlockData for a specific block.
   * @param block the block to set
   * @param state the new block data
   * @return true if block was changed, false otherwise
   */
  default boolean setBlockFast(Block block, BlockState state) {
    block.world().setBlockState(block, state);
    return true;
  }

  private Block eyeBlock(Entity entity) {
    Vector3d loc = entity.location();
    int x = loc.blockX();
    int y = FastMath.floor(loc.y() + (entity.height() * 0.85) - 0.11);
    int z = loc.blockZ();
    return entity.world().blockAt(x, y, z);
  }

  /**
   * Check if an entity is underwater.
   * @param entity the entity to check
   * @return if there is water fluid at the level of the entity's eyes
   */
  default boolean eyeInWater(Entity entity) {
    return MaterialUtil.isWater(eyeBlock(entity));
  }

  /**
   * Check if an entity is under lava.
   * @param entity the entity to check
   * @return if there is lava fluid at the level of the entity's eyes
   */
  default boolean eyeInLava(Entity entity) {
    return MaterialUtil.isLava(eyeBlock(entity));
  }

  /**
   * Try to power a block if it's a lightning rod.
   * @param block the block to power
   * @return true if a lightning rod was powered, false otherwise
   */
  default boolean tryPowerLightningRod(Block block) { // Only native implementation handles continuous lightning strikes
    return block.type() == BlockType.LIGHTNING_ROD;
  }
}
