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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public final class WorldMethods {
	public static List<Block> getNearbyBlocks(Location location, double radius) {
		return getNearbyBlocks(location, radius, block -> true);
	}

	public static List<Block> getNearbyBlocks(Location location, double radius, Set<Material> ignoreMaterials) {
		return getNearbyBlocks(location, radius, block -> !ignoreMaterials.contains(block.getType()));
	}

	public static List<Block> getNearbyBlocks(Location location, double radius, Material type) {
		return getNearbyBlocks(location, radius, block -> block.getType() == type);
	}

	public static List<Block> getNearbyBlocks(Location location, double radius, Predicate<Block> predicate) {
		return getNearbyBlocks(location, radius, predicate, 0);
	}

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
					if (pos.distanceSquared(new Vector(x, y, z)) <= radius * radius) {
						Block block = location.getWorld().getBlockAt(NumberConversions.floor(x), NumberConversions.floor(y), NumberConversions.floor(z));
						if (predicate.test(block)) {
							blocks.add(block);
							if (limit > 0 && blocks.size() >= limit) {
								return blocks;
							}
						}
					}
				}
			}
		}
		return blocks;
	}

	public static Block blockCast(World world, Ray ray, int maxRange, Set<Material> solids) {
		Vector origin = ray.origin.toVector();
		Vector target = ray.direction.toVector();
		BlockIterator it = new BlockIterator(world, origin, target, 0, maxRange);
		Block closestBlock;
		while (it.hasNext()) {
			closestBlock = it.next();
			if (solids.contains(closestBlock.getType())) {
				return closestBlock;
			}
		}
		return world.getBlockAt(ray.origin.toLocation(world));
	}

	public static Optional<Block> rayTraceBlocks(Location location, Vector direction, int range) {
		RayTraceResult result = location.getWorld().rayTraceBlocks(location, direction, range);
		if (result != null && result.getHitBlock() != null) return Optional.of(result.getHitBlock());
		return Optional.empty();
	}

	public static Optional<LivingEntity> getTargetEntity(User user, int range) {
		RayTraceResult result = user.getWorld().rayTraceEntities(user.getEntity().getEyeLocation(), user.getEntity().getLocation().getDirection(), range);
		if (result != null && result.getHitEntity() instanceof LivingEntity)
			return Optional.of((LivingEntity) result.getHitEntity());
		return Optional.empty();
	}

	public static Optional<LivingEntity> getTargetEntity(User user, int range, int raySize) {
		RayTraceResult result = user.getWorld().rayTraceEntities(user.getEntity().getEyeLocation(), user.getEntity().getLocation().getDirection(), range, raySize);
		if (result != null && result.getHitEntity() instanceof LivingEntity)
			return Optional.of((LivingEntity) result.getHitEntity());
		return Optional.empty();
	}

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

	public static double distanceAboveGround(Entity entity) {
		return distanceAboveGround(entity, Collections.emptySet());
	}

	public static double distanceAboveGround(Entity entity, Set<Material> groundMaterials) {
		Vector3 direction = Vector3.MINUS_J;
		BlockIterator it = new BlockIterator(entity.getWorld(), entity.getLocation().toVector(), direction.toVector(), 0, 256);
		AABB entityBounds = AABBUtils.getEntityBounds(entity);
		AABB checkBounds;
		while (it.hasNext()) {
			Block block = it.next();
			if (block.isPassable()) continue;
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

		return Double.MAX_VALUE;
	}
}
