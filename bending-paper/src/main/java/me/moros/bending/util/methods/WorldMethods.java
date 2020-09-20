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

import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class with useful {@link World} related methods. Note: This is not thread-safe.
 */
public final class WorldMethods {
	/**
	 * @return {@link #getNearbyBlocks(Location, double, Predicate, int)} with predicate being always true and no block limit.
	 */
	public static List<Block> getNearbyBlocks(Location location, double radius) {
		return getNearbyBlocks(location, radius, block -> true, 0);
	}

	/**
	 * @return {@link #getNearbyBlocks(Location, double, Predicate, int)} with the given predicate and no block limit.
	 */
	public static List<Block> getNearbyBlocks(Location location, double radius, Predicate<Block> predicate) {
		return getNearbyBlocks(location, radius, predicate, 0);
	}

	/**
	 * Collects all blocks in a sphere that satisfy the given predicate.
	 * Note: Limit is only respected if positive. Otherwise all blocks that satisfy the given predicate are collected.
	 * @param location the center point
	 * @param radius the radius of the sphere
	 * @param predicate the predicate that needs to be satisfied for every block
	 * @param limit the amount of blocks to collect
	 * @return all collected blocks
	 */
	public static List<Block> getNearbyBlocks(Location location, double radius, Predicate<Block> predicate, int limit) {
		int r = NumberConversions.ceil(radius) + 1;
		double originX = location.getX();
		double originY = location.getY();
		double originZ = location.getZ();
		List<Block> blocks = new ArrayList<>();
		Vector pos = location.toVector();
		for (double x = originX - r; x <= originX + r; ++x) {
			for (double y = originY - r; y <= originY + r; ++y) {
				for (double z = originZ - r; z <= originZ + r; ++z) {
					if (pos.distanceSquared(new Vector(x, y, z)) > radius * radius) continue;
					Block block = location.getWorld().getBlockAt(NumberConversions.floor(x), NumberConversions.floor(y), NumberConversions.floor(z));
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
	 * @return {@link #getTarget(World, Ray, Set)} with set defaulting to {@link MaterialUtil#TRANSPARENT_MATERIALS}.
	 */
	public static Location getTarget(World world, Ray ray) {
		return getTarget(world, ray, MaterialUtil.TRANSPARENT_MATERIALS);
	}

	/**
	 * Gets the targeted location.
	 * Note: {@link Ray#direction} is a {@link Vector3} and its length provides the range for the check.
	 * @param world the world to check in
	 * @param ray the ray which holds the origin and direction
	 * @param ignored a set of materials that will be ignored
	 * @return the target location
	 */
	public static Location getTarget(World world, Ray ray, Set<Material> ignored) {
		Location location = ray.origin.toLocation(world);
		Vector direction = ray.direction.toVector().normalize();
		for (double i = 0; i < ray.direction.getNorm() + 1; i++) {
			location.add(direction);
			Block center = location.getBlock();
			List<Block> blocks = Stream.concat(Stream.of(center), BlockMethods.CARDINAL_FACES.stream()
				.map(center::getRelative)).collect(Collectors.toList()); // Construct stream with center and neighbouring blocks
			for (Block block : blocks) {
				if (ignored.contains(block.getType())) continue;
				AABB blockBounds = AABBUtils.getBlockBounds(block);
				if (blockBounds.intersects(ray)) {
					return location;
				}
			}
		}
		return location;
	}

	/**
	 * Gets the first targeted block of specific material using a {@link BlockIterator}.
	 * <p> Note: {@link Ray#direction} is a {@link Vector3} and its length provides the range for the check.
	 * <p> Note 2: This method does not check for region protections. Use {@link SourceUtil#getSource(User, int, Set)} instead.
	 * @param world the world to check in
	 * @param ray the ray which holds the origin and direction
	 * @param solids the set of materials that we are interested in
	 * @return the target block or the last block in the iterator if no block satisfied the material check
	 */
	public static Block blockCast(World world, Ray ray, Set<Material> solids) {
		int range = NumberConversions.floor(ray.direction.getNorm());
		BlockIterator it = new BlockIterator(world, ray.origin.toVector(), ray.direction.toVector(), 0, range);
		while (it.hasNext()) {
			Block closestBlock = it.next();
			if (solids.contains(closestBlock.getType())) {
				return closestBlock;
			}
		}
		return world.getBlockAt(ray.origin.toLocation(world));
	}

	/**
	 * @see World#rayTraceBlocks(Location, Vector, double)
	 */
	public static Optional<Block> rayTraceBlocks(Location location, Vector direction, double range) {
		RayTraceResult result = location.getWorld().rayTraceBlocks(location, direction, range);
		if (result != null && result.getHitBlock() != null) return Optional.of(result.getHitBlock());
		return Optional.empty();
	}

	/**
	 * Gets the provided user's targeted entity (predicate is used to ignore the user's entity).
	 * @see World#rayTraceEntities(Location, Vector, double, Predicate)
	 */
	public static Optional<LivingEntity> getTargetEntity(User user, double range) {
		RayTraceResult result = user.getWorld().rayTraceEntities(user.getEntity().getEyeLocation(), user.getEntity().getLocation().getDirection(), range, e -> !e.equals(user.getEntity()));
		if (result != null && result.getHitEntity() instanceof LivingEntity)
			return Optional.of((LivingEntity) result.getHitEntity());
		return Optional.empty();
	}

	/**
	 * Gets the provided user's targeted entity (predicate is used to ignore the user's entity).
	 * @see World#rayTraceEntities(Location, Vector, double, double, Predicate)
	 */
	public static Optional<LivingEntity> getTargetEntity(User user, double range, int raySize) {
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
	public static boolean isOnGround(Entity entity) {
		final double epsilon = 0.01;
		Location location = entity.getLocation().subtract(0, epsilon, 0);
		AABB entityBounds = AABBUtils.getEntityBounds(entity).at(new Vector3(location));
		for (int x = -1; x <= 1; ++x) {
			for (int z = -1; z <= 1; ++z) {
				Block checkBlock = location.clone().add(x, -epsilon, z).getBlock();
				if (checkBlock.isPassable() || MaterialUtil.isAir(checkBlock.getType())) continue;
				AABB checkBounds = AABBUtils.getBlockBounds(checkBlock).at(new Vector3(checkBlock));
				if (checkBlock.isPassable()) return false;
				if (entityBounds.intersects(checkBounds)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Calculates the distance between an entity and the ground using {@link AABB}.
	 * Uses a {@link BlockIterator} with a max range of 256 blocks.
	 * By default it ignores all passable materials unless they are overriden in the provided set.
	 * @param entity the entity to check
	 * @param groundMaterials the set of materials that are considered ground
	 * @return the distance in blocks between the entity and ground or 256 (max range).
	 */
	public static double distanceAboveGround(Entity entity, Set<Material> groundMaterials) {
		Vector3 direction = Vector3.MINUS_J;
		BlockIterator it = new BlockIterator(entity.getWorld(), entity.getLocation().toVector(), direction.toVector(), 0, 256);
		AABB entityBounds = AABBUtils.getEntityBounds(entity);
		AABB checkBounds;
		while (it.hasNext()) {
			Block block = it.next();
			entityBounds.min().add(direction);
			if (groundMaterials.contains(block.getType())) {
				checkBounds = AABB.BLOCK_BOUNDS;
			} else {
				checkBounds = AABBUtils.getBlockBounds(block);
			}
			if (checkBounds.intersects(entityBounds)) {
				return entity.getLocation().distance(block.getLocation());
			}
		}
		return 256;
	}
}
