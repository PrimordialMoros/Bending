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
import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Flight;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractSpout implements Updatable, SimpleAbility {
	protected final User user;
	protected final Set<Block> ignore = new HashSet<>();
	protected final Flight flight;

	protected Predicate<Block> validBlock = x -> true;
	protected AABB collider;

	protected final int height;
	protected final double maxHeight;
	protected final double speed;

	protected double distance;

	public AbstractSpout(@NonNull User user, double height, double speed) {
		this.user = user;

		this.height = NumberConversions.ceil(height);
		this.speed = speed;

		maxHeight = this.height + 2; // Add a buffer for safety

		this.flight = Flight.get(user);
		this.flight.setFlying(true);
	}

	@Override
	public @NonNull UpdateResult update() {
		Block block = WorldMethods.blockCast(user.getWorld(), new Ray(user.getLocation(), Vector3.MINUS_J), maxHeight, ignore).orElse(null);
		if (block == null || !validBlock.test(block)) {
			return UpdateResult.REMOVE;
		}
		// Remove if player gets too far away from ground.
		distance = user.getLocation().getY() - block.getY();
		if (distance > maxHeight) {
			return UpdateResult.REMOVE;
		}
		flight.setFlying(distance <= height);
		// Create a bounding box for collision that extends through the spout from the ground to the player.
		collider = new AABB(new Vector3(-0.5, -distance, -0.5), new Vector3(0.5, 0, 0.5)).at(user.getLocation());
		render();
		postRender();
		return UpdateResult.CONTINUE;
	}

	@Override
	public boolean onEntityHit(@NonNull Entity entity) {
		return true;
	}

	@Override
	public boolean onBlockHit(@NonNull Block block) {
		return true;
	}

	@Override
	public @NonNull Collider getCollider() {
		return collider;
	}

	public @NonNull Flight getFlight() {
		return flight;
	}

	public static void limitVelocity(@NonNull User user, @NonNull Vector velocity, double speed) {
		if (velocity.lengthSquared() > speed * speed) {
			user.getEntity().setVelocity(velocity.normalize().multiply(speed));
		}
	}
}

