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

package me.moros.bending.api.adapter;

import java.util.Optional;
import java.util.function.Function;

import me.moros.bending.api.collision.raytrace.BlockRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.event.BendingDamageEvent;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.display.DisplayProperties;
import me.moros.bending.api.platform.entity.player.Player;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * Interface for all NMS and Packet shenanigans that Bending takes advantage of.
 */
public interface NativeAdapter extends PacketUtil {
  @Internal
  default boolean damage(BendingDamageEvent event, Function<String, Optional<TranslatableComponent>> translator) {
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

  /**
   * Perform a raytrace for blocks and return the result.
   * @param context the raytrace context
   * @return the result of the performed raytrace
   */
  BlockRayTrace rayTraceBlocks(World world, Context context);

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

  @Override
  default void sendNotification(Player player, Item item, Component title) {
  }

  @Override
  default int createArmorStand(World world, Position center, Item item, Vector3d velocity, boolean gravity) {
    return 0;
  }

  @Override
  default int createFallingBlock(World world, Position center, BlockState state, Vector3d velocity, boolean gravity) {
    return 0;
  }

  @Override
  default int createDisplayEntity(World world, Position center, DisplayProperties<?> properties) {
    return 0;
  }

  @Override
  default void updateDisplayTranslation(World world, Position center, int id, Vector3d translation) {
  }

  @Override
  default void fakeBlock(Block block, BlockState state) {
  }

  @Override
  default void fakeBreak(Block block, byte progress) {
  }

  @Override
  default void destroy(int[] ids) {
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
