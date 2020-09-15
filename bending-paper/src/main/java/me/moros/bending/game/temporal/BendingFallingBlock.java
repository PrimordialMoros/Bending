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

package me.moros.bending.game.temporal;

import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.Vector;

import java.util.Optional;

public class BendingFallingBlock implements Temporary {
	public static final TemporalManager<FallingBlock, BendingFallingBlock> manager = new TemporalManager<>();
	private final FallingBlock fallingBlock;

	private RevertTask revertTask;
	private long revertTime;

	public static void init() {
	}

	private BendingFallingBlock(Location location, BlockData data, Vector velocity, boolean gravity) {
		fallingBlock = location.getWorld().spawnFallingBlock(location, data);
		fallingBlock.setVelocity(velocity);
		fallingBlock.setGravity(gravity);
		fallingBlock.setDropItem(false);
		manager.addEntry(fallingBlock, this);
	}

	public BendingFallingBlock(Location location, BlockData data, long duration) {
		this(location, data, new Vector(), false, duration);
	}

	public BendingFallingBlock(Location location, BlockData data, Vector velocity, boolean gravity, long duration) {
		this(location, data, velocity, gravity);
		revertTime = System.currentTimeMillis() + duration;
		manager.enqueue(this);
	}

	@Override
	public void revert() {
		fallingBlock.remove();
		manager.removeEntry(fallingBlock);
	}

	public FallingBlock getFallingBlock() {
		return fallingBlock;
	}

	@Override
	public long getRevertTime() {
		return revertTime;
	}

	@Override
	public void setRevertTime(long revertTime) {
		this.revertTime = revertTime;
	}

	@Override
	public Optional<Temporary.RevertTask> getRevertTask() {
		return Optional.ofNullable(revertTask);
	}

	@Override
	public void setRevertTask(Temporary.RevertTask task) {
		this.revertTask = task;
	}
}
