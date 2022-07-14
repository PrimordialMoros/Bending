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

package me.moros.bending.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Snow;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class with useful {@link World} related methods. Note: This is not thread-safe.
 */
public final class WorldUtil {
  public static final Set<BlockFace> FACES = Set.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
  public static final Set<BlockFace> SIDES = Set.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH);

  private WorldUtil() {
  }

  /**
   * @return {@link #nearbyBlocks(World, Vector3d, double, Predicate, int)} with predicate being always true and no block limit.
   */
  public static List<Block> nearbyBlocks(World world, Vector3d pos, double radius) {
    return nearbyBlocks(world, pos, radius, block -> true, 0);
  }

  /**
   * @return {@link #nearbyBlocks(World, Vector3d, double, Predicate, int)} with the given predicate and no block limit.
   */
  public static List<Block> nearbyBlocks(World world, Vector3d pos, double radius, Predicate<Block> predicate) {
    return nearbyBlocks(world, pos, radius, predicate, 0);
  }

  /**
   * Collects all blocks in a sphere that satisfy the given predicate.
   * <p> Note: Limit is only respected if positive. Otherwise, all blocks that satisfy the given predicate are collected.
   * @param world the world to check
   * @param pos the center point
   * @param radius the radius of the sphere
   * @param predicate the predicate that needs to be satisfied for every block
   * @param limit the amount of blocks to collect
   * @return all collected blocks
   */
  public static List<Block> nearbyBlocks(World world, Vector3d pos, double radius, Predicate<Block> predicate, int limit) {
    int r = FastMath.ceil(radius) + 1;
    List<Block> blocks = new ArrayList<>();
    for (double x = pos.x() - r; x <= pos.x() + r; x++) {
      for (double y = pos.y() - r; y <= pos.y() + r; y++) {
        for (double z = pos.z() - r; z <= pos.z() + r; z++) {
          Vector3d loc = new Vector3d(x, y, z);
          if (pos.distanceSq(loc) > radius * radius) {
            continue;
          }
          Block block = loc.toBlock(world);
          if (predicate.test(block)) {
            blocks.add(block);
            if (limit > 0 && blocks.size() >= limit) {
              return blocks;
            }
          }
        }
      }
    }
    return blocks;
  }

  /**
   * @return {@link #nearbyBlocks(World, AABB, Predicate, int)} with predicate being always true and no block limit.
   */
  public static List<Block> nearbyBlocks(World world, AABB box) {
    return nearbyBlocks(world, box, block -> true, 0);
  }

  /**
   * @return {@link #nearbyBlocks(World, AABB, Predicate, int)} with the given predicate and no block limit.
   */
  public static List<Block> nearbyBlocks(World world, AABB box, Predicate<Block> predicate) {
    return nearbyBlocks(world, box, predicate, 0);
  }

  /**
   * Collects all blocks inside a bounding box that satisfy the given predicate.
   * <p> Note: Limit is only respected if positive. Otherwise, all blocks that satisfy the given predicate are collected.
   * @param world the world to check
   * @param box the bounding box to check
   * @param predicate the predicate that needs to be satisfied for every block
   * @param limit the amount of blocks to collect
   * @return all collected blocks
   */
  public static List<Block> nearbyBlocks(World world, AABB box, Predicate<Block> predicate, int limit) {
    if (box == AABBUtil.DUMMY_COLLIDER) {
      return List.of();
    }
    List<Block> blocks = new ArrayList<>();
    for (double x = box.min.x(); x <= box.max.x(); x++) {
      for (double y = box.min.y(); y <= box.max.y(); y++) {
        for (double z = box.min.z(); z <= box.max.z(); z++) {
          Vector3d loc = new Vector3d(x, y, z);
          Block block = loc.toBlock(world);
          if (predicate.test(block)) {
            blocks.add(block);
            if (limit > 0 && blocks.size() >= limit) {
              return blocks;
            }
          }
        }
      }
    }
    return blocks;
  }

  public static boolean isDay(World world) {
    return world.getEnvironment() == Environment.NORMAL && world.isDayTime();
  }

  public static boolean isNight(World world) {
    return world.getEnvironment() == Environment.NORMAL && !world.isDayTime();
  }

  /**
   * Try to light a block if it's a furnace, smoker, blast furnace or campfire.
   * @param block the block to light
   */
  public static void tryLightBlock(Block block) {
    BlockState state = block.getState(false);
    boolean light = false;
    if (state instanceof Furnace furnace) {
      if (furnace.getBurnTime() < 800) {
        furnace.setBurnTime((short) 800);
        light = true;
      }
    }
    if (light || MaterialUtil.isCampfire(block)) {
      Lightable data = (Lightable) block.getBlockData();
      if (!data.isLit()) {
        data.setLit(true);
        block.setBlockData(data);
      }
    }
  }

  /**
   * Plays an extinguish particle and sound effect at the given block location.
   * @param block the block to play the effect at
   */
  public static void playLavaExtinguishEffect(Block block) {
    SoundUtil.LAVA_EXTINGUISH.play(block);
    Vector3d center = Vector3d.center(block).add(0, 0.2, 0);
    ParticleUtil.of(Particle.CLOUD, center).count(8).offset(0.3).spawn(block.getWorld());
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
      block.setType(MaterialUtil.isSourceBlock(block) ? Material.OBSIDIAN : Material.COBBLESTONE);
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
      block.setType(Material.AIR);
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        SoundUtil.FIRE_EXTINGUISH.play(block);
      }
      return true;
    } else if (MaterialUtil.isCampfire(block)) {
      Lightable data = (Lightable) block.getBlockData();
      if (data.isLit()) {
        data.setLit(false);
        block.setBlockData(data);
      }
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
      Snow snow;
      if (block.getBlockData() instanceof Snow) {
        snow = (Snow) block.getBlockData();
      } else {
        snow = (Snow) Material.SNOW.createBlockData();
        snow.setLayers(snow.getMaximumLayers());
      }
      if (snow.getLayers() == snow.getMinimumLayers()) {
        block.setType(Material.AIR);
      } else {
        snow.setLayers(snow.getLayers() - 1);
        block.setBlockData(snow);
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
    for (BlockFace face : SIDES) {
      Block adjacent = block.getRelative(face);
      if (MaterialUtil.isWater(adjacent) && MaterialUtil.isSourceBlock(adjacent) && !TempBlock.MANAGER.isTemp(adjacent)) {
        sources++;
      }
    }
    return sources >= 2;
  }

  /**
   * Calculate and collect a ring of blocks.
   * Note: ring blocks are in clockwise order and are unique.
   * @param center the center block
   * @param radius the radius of the circle
   * @return a collection of blocks representing the ring
   */
  public static Collection<Block> createBlockRing(Block center, double radius) {
    Vector3d centerVector = Vector3d.center(center);
    int steps = FastMath.ceil(10 * radius);
    return VectorUtil.circle(Vector3d.PLUS_I.multiply(radius), Vector3d.PLUS_J, steps)
      .stream().map(v -> centerVector.add(v).toBlock(center.getWorld())).distinct().toList();
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
      block.breakNaturally(new ItemStack(Material.AIR));
      return true;
    }
    return false;
  }

  public static Optional<Block> findTopBlock(Block block, int height, Predicate<Block> predicate) {
    for (int i = 1; i <= height; i++) {
      Block check = block.getRelative(BlockFace.UP, i);
      if (!predicate.test(check)) {
        return Optional.of(check.getRelative(BlockFace.DOWN));
      }
    }
    return Optional.empty();
  }

  public static Optional<Block> findBottomBlock(Block block, int height, Predicate<Block> predicate) {
    for (int i = 1; i <= height; i++) {
      Block check = block.getRelative(BlockFace.DOWN, i);
      if (!predicate.test(check)) {
        return Optional.of(check.getRelative(BlockFace.UP));
      }
    }
    return Optional.empty();
  }
}
