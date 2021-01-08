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
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public abstract class AbstractBlockLine implements Updatable {
	private final User user;

	protected final Ray ray;
	protected Vector3 location;

	private final double speed;
	private final double range;
	private final long interval;

	private long nextUpdate;

	public AbstractBlockLine(@NonNull User user, @NonNull Ray ray, double range) {
		this(user, ray, 0, 1, range);
	}

	public AbstractBlockLine(@NonNull User user, @NonNull Ray ray, double speed, double range) {
		this(user, ray, 0, speed, range);
	}

	public AbstractBlockLine(@NonNull User user, @NonNull Ray ray, long interval, double range) {
		this(user, ray, interval, 1, range);
	}

	public AbstractBlockLine(@NonNull User user, @NonNull Ray ray, long interval, double speed, double range) {
		this.user = user;
		this.location = ray.origin.add(ray.direction);
		this.ray = ray;
		this.interval = interval;
		this.speed = FastMath.min(1, speed);
		this.range = range;
	}

	@Override
	public @NonNull UpdateResult update() {
		if (interval >= 50) {
			long time = System.currentTimeMillis();
			if (time <= nextUpdate) return UpdateResult.CONTINUE;
			nextUpdate = time + interval;
		}

		location = location.add(ray.direction.scalarMultiply(speed));
		Block block = location.toBlock(user.getWorld());

		if (!isValidBlock(block)) {
			if (isValidBlock(block.getRelative(BlockFace.UP))) {
				location = location.add(Vector3.PLUS_J);
				block = block.getRelative(BlockFace.UP);
			} else if (isValidBlock(block.getRelative(BlockFace.DOWN))) {
				location = location.add(Vector3.MINUS_J);
				block = block.getRelative(BlockFace.DOWN);
			} else {
				return UpdateResult.REMOVE;
			}
		}

		if (location.distanceSq(ray.origin) > range * range) {
			return UpdateResult.REMOVE;
		}

		if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) {
			return UpdateResult.REMOVE;
		}

		return render(block) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
	}

	protected abstract boolean isValidBlock(@NonNull Block block);

	protected abstract boolean render(@NonNull Block block);
}
