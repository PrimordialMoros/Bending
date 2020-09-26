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
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

public abstract class ParticleStream {
	protected final User user;
	protected final World world;
	protected final Ray ray;

	protected Predicate<Block> canCollide = b -> false;
	protected Collider collider;
	protected Vector3 location;

	protected final double speed;
	protected final double maxRange;
	protected final double collisionRadius;

	protected boolean livingOnly;
	protected boolean hitSelf;

	public ParticleStream(User user, Ray ray, double speed, double collisionRadius) {
		this.user = user;
		this.world = user.getWorld();
		this.ray = ray;

		this.speed = speed;
		this.location = ray.origin;
		this.maxRange = ray.direction.getNormSq();
		this.collisionRadius = collisionRadius;
		this.collider = new Sphere(location, collisionRadius);
		render();
	}

	// Return false to destroy this stream
	public UpdateResult update() {
		location = location.add(ray.direction.normalize().scalarMultiply(speed));
		Block block = location.toBlock(user.getWorld());
		if (location.distanceSq(ray.origin) > maxRange || !Game.getProtectionSystem().canBuild(user, block)) {
			return UpdateResult.REMOVE;
		}
		render();
		postRender();
		collider = new Sphere(location, collisionRadius);
		boolean hitEntity = CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, livingOnly, hitSelf);
		if (hitEntity) return UpdateResult.REMOVE;
		if (!MaterialUtil.isTransparent(block)) {
			AABB blockBounds = AABBUtils.getBlockBounds(block);
			if (canCollide.test(block) || blockBounds.intersects(collider)) {
				if (onBlockHit(block)) return UpdateResult.REMOVE;
			}
		}
		return UpdateResult.CONTINUE;
	}

	public abstract void render();

	public void postRender() {
	}

	public abstract boolean onEntityHit(Entity entity);

	public abstract boolean onBlockHit(Block block);

	public Vector3 getLocation() {
		return location;
	}

	public Location getBukkitLocation() {
		return location.toLocation(world);
	}

	public Collider getCollider() {
		return collider;
	}
}
