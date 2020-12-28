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

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TempBlock implements Temporary {
	private static final Set<Block> GRAVITY_CACHE = new HashSet<>();

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
		if (data.getMaterial().hasGravity()) GRAVITY_CACHE.add(block);
		block.setBlockData(data);
		manager.addEntry(block, this, duration);
	}

	public static Optional<TempBlock> create(@NonNull Block block, @NonNull Material type) {
		return create(block, type.createBlockData(), 0, false);
	}

	public static Optional<TempBlock> create(@NonNull Block block, @NonNull Material type, boolean bendable) {
		return create(block, type.createBlockData(), 0, bendable);
	}

	public static Optional<TempBlock> create(@NonNull Block block, @NonNull BlockData data) {
		return create(block, data, 0, false);
	}

	public static Optional<TempBlock> create(@NonNull Block block, @NonNull BlockData data, boolean bendable) {
		return create(block, data, 0, bendable);
	}

	public static Optional<TempBlock> create(@NonNull Block block, @NonNull Material type, long duration) {
		return create(block, type.createBlockData(), duration, false);
	}

	public static Optional<TempBlock> create(@NonNull Block block, @NonNull Material type, long duration, boolean bendable) {
		return create(block, type.createBlockData(), duration, bendable);
	}

	public static Optional<TempBlock> create(@NonNull Block block, @NonNull BlockData data, long duration) {
		return create(block, data, duration, false);
	}

	public static Optional<TempBlock> create(@NonNull Block block, @NonNull BlockData data, long duration, boolean bendable) {
		if (block instanceof TileState) return Optional.empty();
		if (block.getBlockData() instanceof Waterlogged) {
			Waterlogged waterData = ((Waterlogged) block.getBlockData().clone());
			if (waterData.isWaterlogged() && MaterialUtil.AIR.isTagged(data.getMaterial())) {
				waterData.setWaterlogged(false);
				data = waterData;
			} else if (!waterData.isWaterlogged() && data.getMaterial() == Material.WATER) {
				waterData.setWaterlogged(true);
				data = waterData;
			}
		}

		if (MaterialUtil.TRANSPARENT.isTagged(data)) {
			if (BlockMethods.isInfiniteWater(block)) {
				TempBlock tb = manager.get(block).orElse(null);
				if (tb != null) {
					if (Material.WATER.createBlockData().matches(tb.snapshot.getBlockData())) {
						tb.revert();
					}
				} else {
					block.setType(Material.WATER);
				}
				return Optional.empty();
			}
		}

		return Optional.of(new TempBlock(block, data, duration, bendable));
	}

	@Override
	public void revert() {
		snapshot.getWorld().getChunkAtAsync(getBlock()).thenAccept(r -> snapshot.update(true, false));
		manager.removeEntry(getBlock());
		GRAVITY_CACHE.remove(getBlock());
		if (revertTask != null) revertTask.execute();
	}

	public @NonNull Block getBlock() {
		return snapshot.getBlock();
	}

	public @NonNull BlockData getBlockData() {
		return data;
	}

	public @NonNull BlockState getSnapshot() {
		return snapshot;
	}

	public void overwriteSnapshot(@NonNull BlockData newData) {
		this.snapshot.setBlockData(newData);
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

	public static boolean isTouchingTempBlock(@NonNull Block block) {
		return BlockMethods.MAIN_FACES.stream().map(block::getRelative).anyMatch(manager::isTemp);
	}

	public static boolean isBendable(@NonNull Block block) {
		return manager.get(block).map(TempBlock::isBendable).orElse(true);
	}

	public static boolean isGravityCached(@NonNull Block block) {
		return GRAVITY_CACHE.contains(block);
	}
}
