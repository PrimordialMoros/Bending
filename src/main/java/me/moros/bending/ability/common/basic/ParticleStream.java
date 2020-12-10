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

package me.moros.bending.ability.common.basic;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.function.Predicate;

public abstract class ParticleStream implements Updatable, SimpleAbility {
	protected final User user;
	protected final Ray ray;

	protected Predicate<Block> canCollide = b -> false;
	protected Sphere collider;
	protected Vector3 location;
	protected final Vector3 dir;

	protected boolean livingOnly;
	protected boolean hitSelf;

	protected final double speed;
	protected final double maxRange;
	protected final double collisionRadius;

	public ParticleStream(@NonNull User user, @NonNull Ray ray, double speed, double collisionRadius) {
		this.user = user;
		this.ray = ray;
		this.speed = speed;
		this.location = ray.origin;
		this.maxRange = ray.direction.getNormSq();
		this.collisionRadius = collisionRadius;
		this.collider = new Sphere(location, collisionRadius);
		dir = ray.direction.normalize().scalarMultiply(speed);
		render();
	}

	@Override
	public @NonNull UpdateResult update() {
		location = location.add(dir);
		Block block = location.toBlock(user.getWorld());
		if (location.distanceSq(ray.origin) > maxRange || !Bending.getGame().getProtectionSystem().canBuild(user, block)) {
			return UpdateResult.REMOVE;
		}
		render();
		postRender();
		// Use previous collider for entity checks for visual reasons
		boolean hitEntity = CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, livingOnly, hitSelf);
		if (hitEntity) return UpdateResult.REMOVE;
		collider = collider.at(location);
		if (canCollide.test(block) && onBlockHit(block)) return UpdateResult.REMOVE;
		if (!MaterialUtil.isTransparent(block)) {
			if (AABBUtils.getBlockBounds(block).intersects(collider)) {
				if (onBlockHit(block)) return UpdateResult.REMOVE;
			}
		}
		return UpdateResult.CONTINUE;
	}

	public @NonNull Location getBukkitLocation() {
		return location.toLocation(user.getWorld());
	}

	@Override
	public @NonNull Collider getCollider() {
		return collider;
	}
}
