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

package me.moros.bending.ability.common;

import me.moros.bending.game.Game;
import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Disk;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.Collection;

public abstract class AbstractWheel {
	protected final User user;

	private final Vector3 dir;
	private final AABB box;
	protected final Disk collider;
	protected final Ray ray;
	protected Vector3 location;

	protected final double radius;
	protected double entityCollisionRadius;

	protected AbstractWheel(User user, Ray ray, double radius, double speed) {
		this.user = user;
		this.ray = ray;
		this.location = ray.origin;
		this.radius = radius;
		this.dir = ray.direction.normalize().scalarMultiply(speed);
		box = new AABB(new Vector3(-radius, -radius, -radius), new Vector3(radius, radius, radius)).grow(new Vector3(0.5, 0.5, 0.5));
		AABB bounds = new AABB(new Vector3(-0.15, -radius, -radius), new Vector3(0.15, radius, radius));
		double angle = FastMath.toRadians(user.getEntity().getLocation().getYaw());
		OBB obb = new OBB(bounds, new Rotation(Vector3.PLUS_J, angle, RotationConvention.VECTOR_OPERATOR));
		collider = new Disk(obb, new Sphere(location, radius));
	}

	public Vector3 getLocation() {
		return location;
	}

	protected abstract void render();

	protected void postRender() {
	}

	protected abstract boolean onEntityHit(Entity entity);

	public UpdateResult update() {
		location = location.add(dir);
		if (!Game.getProtectionSystem().canBuild(user, location.toBlock(user.getWorld()))) {
			return UpdateResult.REMOVE;
		}
		if (!resolveMovement(radius)) {
			return UpdateResult.REMOVE;
		}
		if (location.subtract(new Vector3(0, radius + 0.25, 0)).toBlock(user.getWorld()).isLiquid()) {
			return UpdateResult.REMOVE;
		}
		render();
		postRender();
		boolean hit = CollisionUtil.handleEntityCollisions(user, collider.addPosition(location), this::onEntityHit, true);
		return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
	}

	// Try to resolve wheel location by checking collider-block intersections.
	public boolean resolveMovement(double maxResolution) {
		Collection<Block> nearbyBlocks = WorldMethods.getNearbyBlocks(user.getWorld(), box.at(location));
		Collider checkCollider = getCollider();
		// Calculate top and bottom positions and add a small buffer
		double topY = location.getY() + radius + 0.05;
		double bottomY = location.getY() - radius - 0.05;
		for (Block block : nearbyBlocks) {
			AABB blockBounds = AABBUtils.getBlockBounds(block);
			if (blockBounds.intersects(checkCollider)) {
				if (blockBounds.min().getY() > topY) { // Collision on the top part
					return false;
				}
				double resolution = blockBounds.max().getY() - bottomY;
				if (resolution > maxResolution) {
					return false;
				} else {
					location = location.add(new Vector3(0, resolution, 0));
					return checkCollisions(nearbyBlocks);
				}
			}
		}
		// Try to fall if the block below doesn't have a bounding box.
		if (location.setY(bottomY).toBlock(user.getWorld()).isPassable()) {
			Disk tempCollider = collider.addPosition(location.subtract(Vector3.PLUS_J));
			if (nearbyBlocks.stream().map(AABBUtils::getBlockBounds).noneMatch(tempCollider::intersects)) {
				location = location.add(Vector3.MINUS_J);
				return true;
			}
		}
		return checkCollisions(nearbyBlocks);
	}

	private boolean checkCollisions(Collection<Block> nearbyBlocks) {
		// Check if there's any final collisions after all movements.
		Collider checkCollider = getCollider();
		return nearbyBlocks.stream().map(AABBUtils::getBlockBounds).noneMatch(checkCollider::intersects);
	}

	public Collider getCollider() {
		return collider.addPosition(location);
	}
}

