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
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;

import java.util.Optional;

public class TempBlock implements Temporary {
	public static final TemporalManager<Block, TempBlock> manager = new TemporalManager<>();
	private BlockState snapshot;
	private final BlockData data;
	private RevertTask revertTask;
	private final boolean bendable;

	public static void init() {
	}

	private TempBlock(Block block, BlockData data, long duration, boolean bendable) {
		snapshot = block.getState();
		this.data = data;
		this.bendable = bendable;
		manager.get(block).ifPresent(temp -> {
			snapshot = temp.snapshot;
			if (revertTask != null) revertTask.execute();
		});
		block.setBlockData(data);
		manager.addEntry(block, this, duration);
	}

	public static Optional<TempBlock> create(Block block, Material type) {
		return create(block, type.createBlockData(), 0, false);
	}

	public static Optional<TempBlock> create(Block block, Material type, boolean bendable) {
		return create(block, type.createBlockData(), 0, bendable);
	}

	public static Optional<TempBlock> create(Block block, BlockData data) {
		return create(block, data, 0, false);
	}

	public static Optional<TempBlock> create(Block block, BlockData data, boolean bendable) {
		return create(block, data, 0, bendable);
	}

	public static Optional<TempBlock> create(Block block, Material type, long duration) {
		return create(block, type.createBlockData(), duration, false);
	}

	public static Optional<TempBlock> create(Block block, Material type, long duration, boolean bendable) {
		return create(block, type.createBlockData(), duration, bendable);
	}

	public static Optional<TempBlock> create(Block block, BlockData data, long duration) {
		return create(block, data, duration, false);
	}

	public static Optional<TempBlock> create(Block block, BlockData data, long duration, boolean bendable) {
		if (block instanceof TileState) return Optional.empty();
		return Optional.of(new TempBlock(block, data, duration, bendable));
	}

	@Override
	public void revert() {
		snapshot.getWorld().getChunkAtAsync(getBlock()).thenAccept(r -> snapshot.update(true, false));
		manager.removeEntry(getBlock());
		if (revertTask != null) revertTask.execute();
	}

	public Block getBlock() {
		return snapshot.getBlock();
	}

	public BlockData getBlockData() {
		return data;
	}

	public BlockState getSnapshot() {
		return snapshot;
	}

	public void removeWithoutReverting() {
		manager.removeEntry(getBlock());
	}

	public boolean isBendable() {
		return bendable;
	}

	@Override
	public void setRevertTask(RevertTask task) {
		this.revertTask = task;
	}

	public static boolean isTouchingTempBlock(Block block) {
		return BlockMethods.MAIN_FACES.stream().map(block::getRelative).anyMatch(manager::isTemp);
	}

	public static boolean isBendable(Block block) {
		return manager.get(block).map(TempBlock::isBendable).orElse(true);
	}
}
