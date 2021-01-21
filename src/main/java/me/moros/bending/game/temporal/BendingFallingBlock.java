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

package me.moros.bending.game.temporal;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.util.Metadata;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;

public class BendingFallingBlock implements Temporary {
	public static final TemporalManager<FallingBlock, BendingFallingBlock> MANAGER = new TemporalManager<>();
	private final FallingBlock fallingBlock;
	private RevertTask revertTask;

	public static void init() {
	}

	public BendingFallingBlock(@NonNull Location location, @NonNull BlockData data, @NonNull Vector3 velocity, boolean gravity, long duration) {
		fallingBlock = location.getWorld().spawnFallingBlock(location, data);
		fallingBlock.setVelocity(velocity.clampVelocity());
		fallingBlock.setGravity(gravity);
		fallingBlock.setDropItem(false);
		fallingBlock.setMetadata(Metadata.FALLING_BLOCK, Metadata.emptyMetadata());
		MANAGER.addEntry(fallingBlock, this, duration);
	}

	public BendingFallingBlock(@NonNull Location location, @NonNull BlockData data, long duration) {
		this(location, data, Vector3.ZERO, false, duration);
	}

	public BendingFallingBlock(@NonNull Block block, @NonNull BlockData data, @NonNull Vector3 velocity, boolean gravity, long duration) {
		this(block.getLocation().add(0.5, 0, 0.5), data, velocity, gravity, duration);
	}

	public BendingFallingBlock(@NonNull Block block, @NonNull BlockData data, long duration) {
		this(block, data, Vector3.ZERO, false, duration);
	}

	@Override
	public void revert() {
		fallingBlock.remove();
		MANAGER.removeEntry(fallingBlock);
		if (revertTask != null) revertTask.execute();
	}

	public @NonNull FallingBlock getFallingBlock() {
		return fallingBlock;
	}

	@Override
	public void setRevertTask(RevertTask task) {
		this.revertTask = task;
	}

	public @NonNull Vector3 getCenter() {
		return new Vector3(fallingBlock.getLocation()).add(new Vector3(0, 0.5, 0));
	}
}
