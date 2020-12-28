/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Utility class with useful {@link World} related methods. Note: This is not thread-safe.
 */
public final class WorldMethods {
	/**
	 * @return {@link #getNearbyBlocks(Location, double, Predicate, int)} with predicate being always true and no block limit.
	 */
	public static @NonNull Collection<@NonNull Block> getNearbyBlocks(@NonNull Location location, double radius) {
		return getNearbyBlocks(location, radius, block -> true, 0);
	}

	/**
	 * @return {@link #getNearbyBlocks(Location, double, Predicate, int)} with the given predicate and no block limit.
	 */
	public static @NonNull Collection<@NonNull Block> getNearbyBlocks(@NonNull Location location, double radius, @NonNull Predicate<Block> predicate) {
		return getNearbyBlocks(location, radius, predicate, 0);
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
	public static @NonNull Collection<@NonNull Block> getNearbyBlocks(@NonNull Location location, double radius, @NonNull Predicate<Block> predicate, int limit) {
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
					if (pos.distanceSq(loc) > radius * radius) continue;
					Block block = loc.toBlock(location.getWorld());
					if (predicate.test(block)) {
						blocks.add(block);
						if (limit > 0 && blocks.size() >= limit) return blocks;
					}
				}
			}
		}
		return blocks;
	}

	/**
	 * @return {@link #getNearbyBlocks(World, AABB, Predicate, int)} with predicate being always true and no block limit.
	 */
	public static @NonNull Collection<@NonNull Block> getNearbyBlocks(@NonNull World world, @NonNull AABB box) {
		return getNearbyBlocks(world, box, block -> true, 0);
	}

	/**
	 * @return {@link #getNearbyBlocks(World, AABB, Predicate, int)} with the given predicate and no block limit.
	 */
	public static @NonNull Collection<@NonNull Block> getNearbyBlocks(@NonNull World world, @NonNull AABB box, @NonNull Predicate<Block> predicate) {
		return getNearbyBlocks(world, box, predicate, 0);
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
	public static @NonNull Collection<@NonNull Block> getNearbyBlocks(@NonNull World world, @NonNull AABB box, @NonNull Predicate<Block> predicate, int limit) {
		if (box == AABBUtils.DUMMY_COLLIDER) return Collections.emptyList();
		Vector3 min = box.min();
		Vector3 max = box.max();
		List<Block> blocks = new ArrayList<>();
		for (double x = min.getX(); x <= max.getX(); x++) {
			for (double y = min.getY(); y <= max.getY(); y++) {
				for (double z = min.getZ(); z <= max.getZ(); z++) {
					Vector3 loc = new Vector3(x, y, z);
					Block block = loc.toBlock(world);
					if (predicate.test(block)) {
						blocks.add(block);
						if (limit > 0 && blocks.size() >= limit) return blocks;
					}
				}
			}
		}
		return blocks;
	}

	/**
	 * @return {@link #getTarget(World, Ray, Set)} with an empty material set and ignoreLiquids = true
	 */
	public static @NonNull Location getTarget(@NonNull World world, @NonNull Ray ray) {
		return getTarget(world, ray, Collections.emptySet(), true);
	}

	/**
	 * @return {@link #getTarget(World, Ray, Set, boolean)} with an empty material set
	 */
	public static @NonNull Location getTarget(@NonNull World world, @NonNull Ray ray, boolean ignoreLiquids) {
		return getTarget(world, ray, Collections.emptySet(), ignoreLiquids);
	}

	/**
	 * @return {@link #getTarget(World, Ray, Set, boolean)} with ignoreLiquids = true
	 */
	public static @NonNull Location getTarget(@NonNull World world, @NonNull Ray ray, @NonNull Set<@NonNull Material> ignored) {
		return getTarget(world, ray, ignored, true);
	}

	/**
	 * Gets the targeted location.
	 * <p> Note: {@link Ray#direction} is a {@link Vector3} and its length provides the range for the check.
	 * @param world the world to check in
	 * @param ray the ray which holds the origin and direction
	 * @param ignored an extra set of materials that will be ignored (transparent materials are already ignored)
	 * @param ignoreLiquids whether liquids should be ignored for collisions
	 * @return the target location
	 */
	public static @NonNull Location getTarget(@NonNull World world, @NonNull Ray ray, @NonNull Set<@NonNull Material> ignored, boolean ignoreLiquids) {
		Location location = ray.origin.toLocation(world);
		Vector direction = ray.direction.normalize().toVector();
		for (double i = 0; i < ray.direction.getNorm() + 1; i++) {
			Block center = location.getBlock();
			for (Block block : BlockMethods.combineFaces(center)) {
				if (MaterialUtil.isTransparent(block) || ignored.contains(block.getType())) continue;
				AABB blockBounds = (block.isLiquid() && !ignoreLiquids) ? AABB.BLOCK_BOUNDS.at(new Vector3(block)) : AABBUtils.getBlockBounds(block);
				if (blockBounds.intersects(ray)) {
					return location;
				}
			}
			location.add(direction);
		}
		return ray.origin.add(ray.direction).toLocation(world);
	}

	/**
	 * @see #blockCast(World, Ray, double, Set)
	 */
	public static Optional<Block> blockCast(@NonNull World world, @NonNull Ray ray, double range) {
		return blockCast(world, ray, range, Collections.emptySet());
	}

	/**
	 * Ray trace blocks using a {@link BlockIterator}.
	 * <p> Note: Passable blocks except liquids are automatically ignored.
	 * @param world the world to check in
	 * @param ray the ray which holds the origin and direction
	 * @param ignore the set of blocks that will be ignored, passable block types are also ignored.
	 * @return Optional of the result block
	 */
	public static Optional<Block> blockCast(@NonNull World world, @NonNull Ray ray, double range, @NonNull Set<@NonNull Block> ignore) {
		BlockIterator it = new BlockIterator(world, ray.origin.toVector(), ray.direction.toVector(), 0, NumberConversions.floor(range));
		while (it.hasNext()) {
			Block closestBlock = it.next();
			if (closestBlock.isPassable() && !closestBlock.isLiquid()) continue;
			if (!ignore.contains(closestBlock)) {
				return Optional.of(closestBlock);
			}
		}
		return Optional.empty();
	}

	/**
	 * @see World#rayTraceBlocks(Location, Vector, double)
	 */
	public static Optional<Block> rayTraceBlocks(@NonNull Location location, @NonNull Vector direction, double range) {
		RayTraceResult result = location.getWorld().rayTraceBlocks(location, direction, range, FluidCollisionMode.ALWAYS, false);
		if (result != null && result.getHitBlock() != null) return Optional.of(result.getHitBlock());
		return Optional.empty();
	}

	/**
	 * Gets the provided user's targeted entity (predicate is used to ignore the user's entity).
	 * @see World#rayTraceEntities(Location, Vector, double, Predicate)
	 */
	public static Optional<LivingEntity> getTargetEntity(@NonNull User user, double range) {
		RayTraceResult result = user.getWorld().rayTraceEntities(user.getEntity().getEyeLocation(), user.getEntity().getLocation().getDirection(), range, e -> !e.equals(user.getEntity()));
		if (result != null && result.getHitEntity() instanceof LivingEntity)
			return Optional.of((LivingEntity) result.getHitEntity());
		return Optional.empty();
	}

	/**
	 * Gets the provided user's targeted entity (predicate is used to ignore the user's entity).
	 * @see World#rayTraceEntities(Location, Vector, double, double, Predicate)
	 */
	public static Optional<LivingEntity> getTargetEntity(@NonNull User user, double range, int raySize) {
		RayTraceResult result = user.getWorld().rayTraceEntities(user.getEntity().getEyeLocation(), user.getEntity().getLocation().getDirection(), range, raySize, e -> !e.equals(user.getEntity()));
		if (result != null && result.getHitEntity() instanceof LivingEntity)
			return Optional.of((LivingEntity) result.getHitEntity());
		return Optional.empty();
	}

	/**
	 * Accurately checks if an entity is standing on ground using {@link AABB}.
	 * Note: For mobs you should prefer {@link Entity#isOnGround()}. This method is to be used for Players.
	 * @param entity the entity to check
	 * @return true if entity standing on ground, false otherwise
	 */
	public static boolean isOnGround(@NonNull Entity entity) {
		if (!(entity instanceof Player)) return entity.isOnGround();
		AABB entityBounds = AABBUtils.getEntityBounds(entity).grow(new Vector3(0, 0.05, 0));
		AABB floorBounds = new AABB(new Vector3(-1, -0.1, -1), new Vector3(1, 0.1, 1)).at(new Vector3(entity.getLocation()));
		for (Block block : getNearbyBlocks(entity.getWorld(), floorBounds, b -> !b.isPassable())) {
			if (entityBounds.intersects(AABBUtils.getBlockBounds(block))) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Calculates the distance between an entity and the ground using {@link AABB}.
	 * Uses a {@link BlockIterator} with the max world height as the range.
	 * By default it ignores all passable materials except liquids.
	 * @param entity the entity to check
	 * @return the distance in blocks between the entity and ground or the max world height.
	 */
	public static double distanceAboveGround(@NonNull Entity entity) {
		int maxHeight = entity.getWorld().getMaxHeight();
		BlockIterator it = new BlockIterator(entity.getWorld(), entity.getLocation().toVector(), Vector3.MINUS_J.toVector(), 0, 256);
		AABB entityBounds = AABBUtils.getEntityBounds(entity).grow(new Vector3(0, maxHeight, 0));
		while (it.hasNext()) {
			Block block = it.next();
			if (block.getY() <= 0) break;
			AABB checkBounds = block.isLiquid() ? AABB.BLOCK_BOUNDS.at(new Vector3(block)) : AABBUtils.getBlockBounds(block);
			if (checkBounds.intersects(entityBounds)) {
				return FastMath.max(0, entity.getBoundingBox().getMinY() - checkBounds.max().getY());
			}
		}
		return maxHeight;
	}

	/**
	 * Check if a user is against a wall made of blocks matching the given predicate.
	 * <p> Note: Passable blocks and barriers are ignored.
	 * @param user the user to check
	 * @param predicate the type of blocks to accept
	 * @return whether the user is against a wall
	 */
	public static boolean isAgainstWall(@NonNull User user, @NonNull Predicate<Block> predicate) {
		Block origin = user.getLocBlock();
		for (BlockFace face : BlockMethods.CARDINAL_FACES) {
			Block relative = origin.getRelative(face);
			if (relative.isPassable() || relative.getType() == Material.BARRIER) continue;
			if (predicate.test(relative)) {
				return true;
			}
		}
		return false;
	}
}
