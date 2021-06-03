/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.util.methods;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Snow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class with useful {@link Block} related methods. Note: This is not thread-safe.
 */
public final class BlockMethods {
  public static final Set<BlockFace> MAIN_FACES = Set.of(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN);
  public static final Set<BlockFace> CARDINAL_FACES = Set.of(BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH);

  private BlockMethods() {
  }

  /**
   * Try to light a block if it's a furnace, smoker, blast furnace or campfire.
   * @param block the block to light
   */
  public static void tryLightBlock(@NonNull Block block) {
    BlockState state = block.getState(false);
    boolean light = false;
    if (state instanceof Furnace) {
      if (((Furnace) state).getBurnTime() < 800) {
        ((Furnace) state).setBurnTime((short) 800);
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
  public static void playLavaExtinguishEffect(@NonNull Block block) {
    Location center = block.getLocation().add(0.5, 0.7, 0.5);
    SoundUtil.LAVA_EXTINGUISH.play(center);
    ParticleUtil.create(Particle.CLOUD, center).count(8)
      .offset(0.3, 0.3, 0.3).spawn();
  }

  /**
   * Try to cool down the given block if it's Lava.
   * @param user the user trying to cool the lava
   * @param block the block to check
   * @return true if lava was cooled down, false otherwise
   */
  public static boolean tryCoolLava(@NonNull User user, @NonNull Block block) {
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
  public static boolean tryExtinguishFire(@NonNull User user, @NonNull Block block) {
    if (!user.canBuild(block)) {
      return false;
    }
    if (MaterialUtil.isFire(block)) {
      block.setType(Material.AIR);
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        SoundUtil.FIRE_EXTINGUISH.play(block.getLocation());
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
  public static boolean tryMelt(@NonNull User user, @NonNull Block block) {
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
      TempBlock.MANAGER.get(block).ifPresentOrElse(TempBlock::revert, () -> TempBlock.createAir(block));
      return true;
    }
    return false;
  }

  /**
   * Check surrounding blocks to see if an infinite water source can be created.
   * @param block the center block to check
   * @return true if there 2 or more water sources around the block
   */
  public static boolean isInfiniteWater(@NonNull Block block) {
    int sources = 0;
    for (BlockFace face : CARDINAL_FACES) {
      Block adjacent = block.getRelative(face);
      if (!TempBlock.isBendable(adjacent)) {
        continue;
      }
      if (MaterialUtil.isWater(adjacent) && MaterialUtil.isSourceBlock(adjacent)) {
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
  public static @NonNull Collection<Block> createBlockRing(@NonNull Block center, double radius) {
    Vector3 centerVector = Vector3.center(center);
    int steps = NumberConversions.ceil(10 * radius);
    return VectorMethods.circle(Vector3.PLUS_I.multiply(radius), Vector3.PLUS_J, steps)
      .stream().map(v -> centerVector.add(v).toBlock(center.getWorld())).distinct().collect(Collectors.toList());
  }

  /**
   * Try to break the specified block if it's a valid plant ({@link MaterialUtil#BREAKABLE_PLANTS}).
   * @param block the block to break
   * @return true if the plant was broken, false otherwise
   */
  public static boolean tryBreakPlant(@NonNull Block block) {
    if (MaterialUtil.BREAKABLE_PLANTS.isTagged(block)) {
      if (TempBlock.MANAGER.isTemp(block)) {
        return false;
      }
      block.breakNaturally(new ItemStack(Material.AIR));
      return true;
    }
    return false;
  }

  public static Optional<Block> findTopBlock(@NonNull Block block, int height, @NonNull Predicate<Block> predicate) {
    for (int i = 1; i <= height; i++) {
      Block check = block.getRelative(BlockFace.UP, i);
      if (!predicate.test(check)) {
        return Optional.of(check.getRelative(BlockFace.DOWN));
      }
    }
    return Optional.empty();
  }

  public static Optional<Block> findBottomBlock(Block block, int height, @NonNull Predicate<Block> predicate) {
    for (int i = 1; i <= height; i++) {
      Block check = block.getRelative(BlockFace.DOWN, i);
      if (!predicate.test(check)) {
        return Optional.of(check.getRelative(BlockFace.UP));
      }
    }
    return Optional.empty();
  }
}
