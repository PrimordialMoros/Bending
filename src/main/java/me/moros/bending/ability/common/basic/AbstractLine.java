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

package me.moros.bending.ability.common.basic;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Predicate;

public abstract class AbstractLine implements Updatable, SimpleAbility {
	private final User user;

	protected final Vector3 origin;

	protected Predicate<Block> diagonalsPredicate = b -> !MaterialUtil.isTransparent(b);
	protected Vector3 location;
	protected Vector3 targetLocation;
	protected Vector3 direction;
	protected Collider collider;
	protected LivingEntity target;

	protected final double range;
	protected final double speed;

	protected boolean locked = false;
	protected boolean controllable = false;
	protected boolean skipVertical = false;

	public AbstractLine(@NonNull User user, @NonNull Block source, double range, double speed, boolean followTarget) {
		this.user = user;
		this.location = new Vector3(source.getLocation().add(0.5, 1.25, 0.5));
		this.origin = location;
		this.range = range;
		this.speed = speed;
		Optional<LivingEntity> entity = user.getTargetEntity(range);
		if (followTarget && entity.isPresent()) {
			target = entity.get();
			locked = true;
		}
		targetLocation = entity.map(EntityMethods::getEntityCenter).orElseGet(() ->
			WorldMethods.getTarget(user.getWorld(), user.getRay(range), Collections.singleton(Material.WATER))
		);
		direction = targetLocation.subtract(location).setY(0).normalize();
	}

	@Override
	public @NonNull UpdateResult update() {
		if (locked) {
			if (isValidTarget()) {
				targetLocation = new Vector3(target.getLocation());
				direction = targetLocation.subtract(location).setY(0).normalize();
			} else {
				locked = false;
			}
		}

		if (controllable) {
			targetLocation = WorldMethods.getTarget(user.getWorld(), user.getRay(range));
			direction = targetLocation.subtract(origin).setY(0).normalize();
		}

		if (onBlockHit(location.toBlock(user.getWorld()).getRelative(BlockFace.DOWN))) {
			return UpdateResult.REMOVE;
		}

		collider = new Sphere(location, 1);
		if (CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true)) {
			return UpdateResult.REMOVE;
		}

		render();
		postRender();

		Vector3 originalVector = new Vector3(location.toArray());
		location = location.add(direction.scalarMultiply(speed));
		Block baseBlock = location.toBlock(user.getWorld()).getRelative(BlockFace.DOWN);

		if (!isValidBlock(baseBlock)) {
			if (isValidBlock(baseBlock.getRelative(BlockFace.UP))) {
				location = location.add(Vector3.PLUS_J);
			} else if (isValidBlock(baseBlock.getRelative(BlockFace.DOWN))) {
				location = location.add(Vector3.MINUS_J);
			} else {
				onCollision();
				return UpdateResult.REMOVE;
			}
		} else if (skipVertical) { // Advance location vertically if possible to match target height
			int y1 = NumberConversions.floor(targetLocation.getY());
			int y2 = NumberConversions.floor(location.getY());
			if (y1 > y2 && isValidBlock(baseBlock.getRelative(BlockFace.UP))) {
				location = location.add(Vector3.PLUS_J);
			} else if (y1 < y2 && isValidBlock(baseBlock.getRelative(BlockFace.DOWN))) {
				location = location.add(Vector3.MINUS_J);
			}
		}

		Block originBlock = originalVector.toBlock(user.getWorld());
		for (Vector3 v : VectorMethods.decomposeDiagonals(originalVector, direction.scalarMultiply(speed))) {
			int x = NumberConversions.floor(v.getX());
			int y = NumberConversions.floor(v.getY());
			int z = NumberConversions.floor(v.getZ());
			if (diagonalsPredicate.test(originBlock.getRelative(x, y, z))) {
				return UpdateResult.REMOVE;
			}
		}

		if (location.distanceSq(origin) > range * range) {
			return UpdateResult.REMOVE;
		}
		if (!Bending.getGame().getProtectionSystem().canBuild(user, location.toBlock(user.getWorld()))) {
			return UpdateResult.REMOVE;
		}
		return UpdateResult.CONTINUE;
	}

	@Override
	public @NonNull Collider getCollider() {
		return collider;
	}

	protected void onCollision() {
	}

	protected abstract boolean isValidBlock(@NonNull Block block);

	protected boolean isValidTarget() {
		if (target == null || !target.isValid()) return false;
		if (target instanceof Player && !((Player) target).isOnline()) return false;
		return target.getWorld().equals(user.getWorld()) && targetLocation.distanceSq(new Vector3(target.getLocation())) < 5 * 5;
	}
}
