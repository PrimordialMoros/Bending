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

package me.moros.bending.api.util;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.property.StateProperty;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;

/**
 * Utility class with useful {@link World} related methods.
 * <p>Note: This is not thread-safe.
 */
public final class WorldUtil {
  public static final Set<Direction> FACES = Set.of(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN);
  public static final Set<Direction> SIDES = Set.of(Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH);

  private WorldUtil() {
  }

  /**
   * Try to light a block if it's a campfire.
   * @param block the block to light
   */
  public static void tryLightBlock(Block block) {
    if (MaterialUtil.isCampfire(block)) {
      block.setState(block.state().withProperty(StateProperty.LIT, true));
    }
  }

  /**
   * Plays an extinguish particle and sound effect at the given block location.
   * @param block the block to play the effect at
   */
  public static void playLavaExtinguishEffect(Block block) {
    SoundEffect.LAVA_EXTINGUISH.play(block);
    Vector3d center = block.center().add(0, 0.2, 0);
    Particle.CLOUD.builder(center).count(8).offset(0.3).spawn(block.world());
  }

  /**
   * Try to cool down the given block if it's Lava.
   * @param user the user trying to cool the lava
   * @param block the block to check
   * @return true if lava was cooled down, false otherwise
   */
  public static boolean tryCoolLava(User user, Block block) {
    if (!user.canBuild(block)) {
      return false;
    }
    if (MaterialUtil.isLava(block)) {
      block.setType(MaterialUtil.isSourceBlock(block) ? BlockType.OBSIDIAN : BlockType.COBBLESTONE);
      if (ThreadLocalRandom.current().nextBoolean()) {
        playLavaExtinguishEffect(block);
      }
      return true;
    }
    return false;
  }

  /**
   * Try to extinguish the given block if it's Fire.
   * @param user the user trying to extinguish the fire
   * @param block the block to check
   * @return true if fire was extinguished, false otherwise
   */
  public static boolean tryExtinguishFire(User user, Block block) {
    if (!user.canBuild(block)) {
      return false;
    }
    if (MaterialUtil.isFire(block)) {
      block.setType(BlockType.AIR);
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        SoundEffect.FIRE_EXTINGUISH.play(block);
      }
      return true;
    } else if (MaterialUtil.isCampfire(block)) {
      block.setState(block.state().withProperty(StateProperty.LIT, false));
    }
    return false;
  }

  /**
   * Try to melt the given block if it's Snow or Ice.
   * @param user the user trying to melt
   * @param block the block to check
   * @return true if snow or ice was melted, false otherwise
   */
  public static boolean tryMelt(User user, Block block) {
    if (!user.canBuild(block)) {
      return false;
    }
    if (WaterMaterials.isSnowBendable(block)) {
      BlockState snow;
      int level;
      if (block.type() == BlockType.SNOW_BLOCK) {
        snow = BlockType.SNOW.defaultState();
        level = StateProperty.LAYERS.max();
      } else {
        snow = block.state();
        level = Objects.requireNonNull(snow.property(StateProperty.LAYERS));
      }
      if (level == StateProperty.LAYERS.min()) {
        block.setType(BlockType.AIR);
      } else {
        block.setState(snow.withProperty(StateProperty.LAYERS, level - 1));
      }
      return true;
    } else if (WaterMaterials.isIceBendable(block)) {
      TempBlock.MANAGER.get(block).ifPresentOrElse(TempBlock::revert, () -> TempBlock.air().build(block));
      return true;
    }
    return false;
  }

  /**
   * Check surrounding blocks to see if an infinite water source can be created.
   * @param block the center block to check
   * @return true if there are 2 or more water sources around the block
   */
  public static boolean isInfiniteWater(Block block) {
    int sources = 0;
    for (Direction face : SIDES) {
      Block adjacent = block.offset(face);
      if (MaterialUtil.isWater(adjacent) && MaterialUtil.isSourceBlock(adjacent) && !TempBlock.MANAGER.isTemp(adjacent)) {
        sources++;
      }
    }
    return sources >= 2;
  }

  /**
   * Calculate and collect a ring of blocks.
   * <p>Note: Ring blocks are in clockwise order and are unique.
   * @param center the center block
   * @param radius the radius of the circle
   * @return a collection of blocks representing the ring
   */
  public static Collection<Block> createBlockRing(Block center, double radius) {
    Vector3d centerVector = center.center();
    int steps = FastMath.ceil(10 * radius);
    return VectorUtil.circle(Vector3d.PLUS_I.multiply(radius), Vector3d.PLUS_J, steps)
      .stream().map(v -> center.world().blockAt(centerVector.add(v))).distinct().toList();
  }

  /**
   * Try to break the specified block if it's a valid plant ({@link MaterialUtil#BREAKABLE_PLANTS}).
   * @param block the block to break
   * @return true if the plant was broken, false otherwise
   */
  public static boolean tryBreakPlant(Block block) {
    if (MaterialUtil.BREAKABLE_PLANTS.isTagged(block)) {
      if (TempBlock.MANAGER.isTemp(block)) {
        return false;
      }
      block.world().breakNaturally(block);
      return true;
    }
    return false;
  }
}
