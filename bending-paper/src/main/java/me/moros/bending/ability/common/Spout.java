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

import me.moros.bending.model.ability.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Flight;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.NumberConversions;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public abstract class Spout {
	protected final User user;
	protected final World world;

	protected Predicate<Block> validBlock = x -> true;
	protected Set<Block> ignore = new HashSet<>();
	protected AABB collider;
	protected Flight flight;

	protected final int height;
	protected final double maxHeight;
	protected final double speed;

	public Spout(User user, double height, double speed) {
		this.user = user;
		this.world = user.getWorld();

		this.height = NumberConversions.ceil(height);
		this.speed = speed;

		maxHeight = this.height + 2; // Add a buffer for safety

		this.flight = Flight.get(user);
		this.flight.setFlying(true);
	}

	public UpdateResult update() {
		Block block = WorldMethods.blockCast(world, new Ray(user.getLocation(), Vector3.MINUS_J), maxHeight, ignore).orElse(null);
		if (block == null || !validBlock.test(block)) {
			return UpdateResult.REMOVE;
		}
		// Remove if player gets too far away from ground.
		double distance = user.getLocation().getY() - block.getY();
		if (distance > maxHeight) {
			return UpdateResult.REMOVE;
		}
		flight.setFlying(distance <= height);
		// Create a bounding box for collision that extends through the spout from the ground to the player.
		collider = new AABB(new Vector3(-0.5, -distance, -0.5), new Vector3(0.5, 0, 0.5)).at(user.getLocation());
		render(distance);
		postRender();
		return UpdateResult.CONTINUE;
	}

	public abstract void render(double distance);

	public void postRender() {
	}

	public Flight getFlight() {
		return flight;
	}

	public Collider getCollider() {
		return collider;
	}

	public static void limitVelocity(User user, Vector velocity, double speed) {
		if (velocity.lengthSquared() > speed * speed) {
			user.getEntity().setVelocity(velocity.normalize().multiply(speed));
		}
	}
}

