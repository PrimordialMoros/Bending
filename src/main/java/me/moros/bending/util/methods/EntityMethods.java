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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.util.collision.AABBUtils;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import java.util.function.Predicate;

/**
 * Utility class with useful {@link Entity} related methods. Note: This is not thread-safe.
 */
public final class EntityMethods {
	/**
	 * Check if a user is against a wall made of blocks matching the given predicate.
	 * <p> Note: Passable blocks and barriers are ignored.
	 * @param entity the entity to check
	 * @param predicate the type of blocks to accept
	 * @return whether the user is against a wall
	 */
	public static boolean isAgainstWall(@NonNull Entity entity, @NonNull Predicate<Block> predicate) {
		Block origin = entity.getLocation().getBlock();
		for (BlockFace face : BlockMethods.CARDINAL_FACES) {
			Block relative = origin.getRelative(face);
			if (relative.isPassable() || relative.getType() == Material.BARRIER) continue;
			if (predicate.test(relative)) {
				return true;
			}
		}
		return false;
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
		for (Block block : WorldMethods.getNearbyBlocks(entity.getWorld(), floorBounds, b -> !b.isPassable())) {
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
	 * Calculates a vector at the center of the given entity using its height.
	 * @param entity the entity to get the vector for
	 * @return the resulting vector
	 */
	public static @NonNull Vector3 getEntityCenter(@NonNull Entity entity) {
		return new Vector3(entity.getLocation()).add(new Vector3(0, entity.getHeight() / 2, 0));
	}
}
