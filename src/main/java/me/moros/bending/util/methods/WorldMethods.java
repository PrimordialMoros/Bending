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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.util.collision.AABBUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class with useful {@link World} related methods. Note: This is not thread-safe.
 */
public final class WorldMethods {
  private WorldMethods() {
  }

  /**
   * @return {@link #nearbyBlocks(Location, double, Predicate, int)} with predicate being always true and no block limit.
   */
  public static @NonNull List<@NonNull Block> nearbyBlocks(@NonNull Location location, double radius) {
    return nearbyBlocks(location, radius, block -> true, 0);
  }

  /**
   * @return {@link #nearbyBlocks(Location, double, Predicate, int)} with the given predicate and no block limit.
   */
  public static @NonNull List<@NonNull Block> nearbyBlocks(@NonNull Location location, double radius, @NonNull Predicate<Block> predicate) {
    return nearbyBlocks(location, radius, predicate, 0);
  }

  /**
   * Collects all blocks in a sphere that satisfy the given predicate.
   * <p> Note: Limit is only respected if positive. Otherwise all blocks that satisfy the given predicate are collected.
   * @param location the center point
   * @param radius the radius of the sphere
   * @param predicate the predicate that needs to be satisfied for every block
   * @param limit the amount of blocks to collect
   * @return all collected blocks
   */
  public static @NonNull List<@NonNull Block> nearbyBlocks(@NonNull Location location, double radius, @NonNull Predicate<Block> predicate, int limit) {
    int r = NumberConversions.ceil(radius) + 1;
    double originX = location.getX();
    double originY = location.getY();
    double originZ = location.getZ();
    Vector3 pos = new Vector3(location);
    List<Block> blocks = new ArrayList<>();
    for (double x = originX - r; x <= originX + r; x++) {
      for (double y = originY - r; y <= originY + r; y++) {
        for (double z = originZ - r; z <= originZ + r; z++) {
          Vector3 loc = new Vector3(x, y, z);
          if (pos.distanceSq(loc) > radius * radius) {
            continue;
          }
          Block block = loc.toBlock(location.getWorld());
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
  public static @NonNull List<@NonNull Block> nearbyBlocks(@NonNull World world, @NonNull AABB box) {
    return nearbyBlocks(world, box, block -> true, 0);
  }

  /**
   * @return {@link #nearbyBlocks(World, AABB, Predicate, int)} with the given predicate and no block limit.
   */
  public static @NonNull List<@NonNull Block> nearbyBlocks(@NonNull World world, @NonNull AABB box, @NonNull Predicate<Block> predicate) {
    return nearbyBlocks(world, box, predicate, 0);
  }

  /**
   * Collects all blocks inside a bounding box that satisfy the given predicate.
   * <p> Note: Limit is only respected if positive. Otherwise all blocks that satisfy the given predicate are collected.
   * @param world the world to check
   * @param box the bounding box to check
   * @param predicate the predicate that needs to be satisfied for every block
   * @param limit the amount of blocks to collect
   * @return all collected blocks
   */
  public static @NonNull List<@NonNull Block> nearbyBlocks(@NonNull World world, @NonNull AABB box, @NonNull Predicate<Block> predicate, int limit) {
    if (box == AABBUtils.DUMMY_COLLIDER) {
      return List.of();
    }
    List<Block> blocks = new ArrayList<>();
    for (double x = box.min.x; x <= box.max.x; x++) {
      for (double y = box.min.y; y <= box.max.y; y++) {
        for (double z = box.min.z; z <= box.max.z; z++) {
          Vector3 loc = new Vector3(x, y, z);
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
   * @return {@link #rayTraceBlocks(World, Ray, double, boolean, boolean)} ignoring liquids and passable blocks
   */
  public static Optional<Block> rayTraceBlocks(@NonNull World world, @NonNull Ray ray, double range) {
    return rayTraceBlocks(world, ray, range, true, true);
  }

  /**
   * Ray trace blocks using their precise colliders.
   * @param world the world to check in
   * @param ray the ray which holds the origin and direction
   * @param range the maximum range to check
   * @param ignoreLiquids whether liquids should be ignored
   * @param ignorePassable whether passable blocks should be ignored
   * @return Optional of the result block
   */
  public static Optional<Block> rayTraceBlocks(@NonNull World world, @NonNull Ray ray, double range, boolean ignoreLiquids, boolean ignorePassable) {
    Location origin = ray.origin.toLocation(world);
    Vector dir = ray.direction.toBukkitVector();
    FluidCollisionMode fluid = ignoreLiquids ? FluidCollisionMode.NEVER : FluidCollisionMode.ALWAYS;
    RayTraceResult result = world.rayTraceBlocks(origin, dir, range, fluid, ignorePassable);
    return result == null ? Optional.empty() : Optional.ofNullable(result.getHitBlock());
  }

  public static boolean isDay(@NonNull World world) {
    if (world.getEnvironment() != World.Environment.NORMAL) {
      return false;
    }
    return world.isDayTime();
  }

  public static boolean isNight(@NonNull World world) {
    if (world.getEnvironment() != World.Environment.NORMAL) {
      return false;
    }
    return !world.isDayTime();
  }
}
