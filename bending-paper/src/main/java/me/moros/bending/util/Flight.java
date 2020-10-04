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

package me.moros.bending.util;

import me.moros.bending.model.user.BendingUser;
import me.moros.bending.model.user.User;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a reference counting object that's used to manage a user's flight.
 * Every time a reference is acquired, it should eventually be released.
 * If the reference count drops below 1 then the user will lose flight.
 */
public class Flight {
	private static final Map<User, Flight> instances = new HashMap<>();

	private final User user;
	private int references;
	private final boolean couldFly;
	private final boolean wasFlying;
	private boolean isFlying;
	private boolean changedFlying;

	private Flight(User user) {
		this.user = user;
		references = 0;
		isFlying = false;
		changedFlying = false;
		couldFly = user.getAllowFlight();
		wasFlying = user.isFlying();
	}

	// Returns the Flight instance for a user. This will also increment the flight counter.
	// Call release() to decrement the counter.
	// Call remove() to completely remove flight.
	public static Flight get(User user) {
		return instances.computeIfAbsent(user, Flight::new).increment();
	}

	private Flight increment() {
		++references;
		return this;
	}

	public void setFlying(boolean value) {
		isFlying = value;
		user.setAllowFlight(value);
		user.setFlying(value);
		changedFlying = true;
	}

	public User getUser() {
		return user;
	}

	// Decrements the user's flight counter. If this goes below 1 then the user loses flight.
	public void release() {
		if (--references < 1) remove(user);
	}

	public static boolean hasFlight(User user) {
		return instances.containsKey(user);
	}

	// Completely releases flight for the user.
	// This will set the user back to the state before any Flight was originally added.
	public static void remove(User user) {
		if (!instances.containsKey(user)) return;
		Flight flight = instances.remove(user);
		if (flight.changedFlying) {
			user.setAllowFlight(flight.couldFly);
			user.setFlying(flight.wasFlying);
		}
	}

	public static void removeAll() {
		for (Map.Entry<User, Flight> entry : instances.entrySet()) {
			User user = entry.getKey();
			Flight flight = entry.getValue();
			if (flight.changedFlying) {
				user.setAllowFlight(flight.couldFly);
				user.setFlying(flight.wasFlying);
			}
		}
		instances.clear();
	}

	public static void updateAll() {
		for (Map.Entry<User, Flight> entry : instances.entrySet()) {
			User user = entry.getKey();
			Flight flight = entry.getValue();
			if (flight.changedFlying && user.isFlying() != flight.isFlying) {
				user.setFlying(flight.isFlying);
			}
		}
	}

	// This class will apply flight when constructed and then remove it when the user touches the ground.
	public static class GroundRemovalTask implements Runnable {
		private final Flight flight;
		private final BendingUser user;
		private final long start;
		private final long maxDuration;
		private boolean cancelled;

		private final BukkitTask task;

		public GroundRemovalTask(BendingUser user, int initialDelay) {
			this(user, initialDelay, 10000L);
		}

		public GroundRemovalTask(BendingUser user, int initialDelay, long maxDuration) {
			this.user = user;
			this.maxDuration = maxDuration;
			flight = Flight.get(user);
			start = System.currentTimeMillis();
			cancelled = false;
			task = Tasker.createTaskTimer(this, initialDelay, 1);
		}

		@Override
		public void run() {
			if (cancelled) {
				flight.release();
				task.cancel();
				return;
			}
			long time = System.currentTimeMillis();
			if (time >= start + maxDuration || WorldMethods.isOnGround(user.getEntity())) {
				cancelled = true; // Remove flight next tick so Flight still exists during the fall event handler.
			}
		}
	}
}
