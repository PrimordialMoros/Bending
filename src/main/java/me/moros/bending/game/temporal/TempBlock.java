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
import me.moros.atlas.expiringmap.ExpirationPolicy;
import me.moros.atlas.expiringmap.ExpiringMap;
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
import java.util.concurrent.TimeUnit;

public class TempBlock implements Temporary {
	private static final Set<Block> GRAVITY_CACHE = new HashSet<>();

	public static final TemporalManager<Block, TempBlock> MANAGER = new TemporalManager<>();
	private static final ExpiringMap<Block, Boolean> TEMP_AIR = ExpiringMap.builder().variableExpiration().build();

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
		TempBlock temp = MANAGER.get(block).orElse(null);
		if (temp != null) {
			snapshot = temp.snapshot;
			if (temp.revertTask != null) temp.revertTask.execute();
		} else {
			if (data.getMaterial().isAir() && !TEMP_AIR.containsKey(block)) {
				TEMP_AIR.put(block, false, ExpirationPolicy.CREATED, duration <= 0 ? DEFAULT_REVERT : duration, TimeUnit.MILLISECONDS);
			}
		}
		if (data.getMaterial().hasGravity()) GRAVITY_CACHE.add(block);
		block.setBlockData(data);
		MANAGER.addEntry(block, this, duration);
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

		TempBlock tb = MANAGER.get(block).orElse(null);
		if (tb != null && data.matches(tb.snapshot.getBlockData())) {
			return Optional.empty();
		}

		if (block.getBlockData() instanceof Waterlogged) {
			Waterlogged waterData = ((Waterlogged) block.getBlockData().clone());
			if (waterData.isWaterlogged() && data.getMaterial().isAir()) {
				waterData.setWaterlogged(false);
				data = waterData;
			} else if (!waterData.isWaterlogged() && data.getMaterial() == Material.WATER) {
				waterData.setWaterlogged(true);
				data = waterData;
			}
		}

		if (MaterialUtil.TRANSPARENT.isTagged(data)) {
			if (BlockMethods.isInfiniteWater(block)) {
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
		Block block = getBlock();
		if (TEMP_AIR.containsKey(block)) {
			long remainingTime = TEMP_AIR.getExpectedExpiration(block);
			create(block, Material.AIR, remainingTime, true);
			return;
		}
		snapshot.getWorld().getChunkAtAsync(block).thenAccept(r -> snapshot.update(true, false));
		MANAGER.removeEntry(block);
		GRAVITY_CACHE.remove(block);
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
		MANAGER.removeEntry(getBlock());
	}

	public boolean isBendable() {
		return bendable;
	}

	@Override
	public void setRevertTask(RevertTask task) {
		this.revertTask = task;
	}

	public static boolean isTouchingTempBlock(@NonNull Block block) {
		return BlockMethods.MAIN_FACES.stream().map(block::getRelative).anyMatch(MANAGER::isTemp);
	}

	public static boolean isBendable(@NonNull Block block) {
		return MANAGER.get(block).map(TempBlock::isBendable).orElse(true);
	}

	public static boolean isGravityCached(@NonNull Block block) {
		return GRAVITY_CACHE.contains(block);
	}

	public static void clearAir() {
		TEMP_AIR.clear();
	}
}
