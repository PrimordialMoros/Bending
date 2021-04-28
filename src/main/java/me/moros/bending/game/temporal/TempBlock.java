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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;

public class TempBlock implements Temporary {
  private static final Set<Block> GRAVITY_CACHE = ConcurrentHashMap.newKeySet();

  public static final TemporalManager<Block, TempBlock> MANAGER = new TemporalBlockManager();

  private final Deque<TempBlockState> snapshots = new ArrayDeque<>();
  private final Block block;
  private boolean bendable;

  public static void init() {
  }

  private TempBlock(Block block, boolean bendable) {
    TemporalBlockManager.CACHE.add(block);
    this.block = block;
    this.bendable = bendable;
  }

  private TempBlock addState(BlockData data, long duration) {
    cleanStates();
    TempBlockState tbs = snapshots.peekLast();
    if (tbs == null || !tbs.overwrite) {
      snapshots.offerLast(new TempBlockState(block.getState(false), duration, bendable));
    } else {
      tbs.overwrite = false;
    }
    block.setBlockData(data);
    refreshGravityCache(block);
    MANAGER.addEntry(block, this, duration);
    return this;
  }

  // Cleans up previous states that have already expired
  private void cleanStates() {
    if (snapshots.size() < 2) {
      return;
    }
    long time = System.currentTimeMillis();
    Iterator<TempBlockState> it = snapshots.iterator();
    it.next(); // ignore original snapshot
    while (it.hasNext()) {
      TempBlockState tbs = it.next();
      if (time > tbs.expirationTime + 5000) {
        it.remove();
      }
    }
  }

  public static @NonNull BlockData getLastValidData(@NonNull Block block) {
    TempBlock tb = MANAGER.get(block).orElse(null);
    if (tb != null) {
      TempBlockState tbs = tb.snapshots.peekLast();
      if (tbs != null && tbs.overwrite) {
        return tbs.state.getBlockData();
      }
    }
    return block.getBlockData();
  }

  public static Optional<TempBlock> create(@NonNull Block block, @NonNull BlockData data) {
    return create(block, data, 0, false);
  }

  public static Optional<TempBlock> create(@NonNull Block block, @NonNull BlockData data, boolean bendable) {
    return create(block, data, 0, bendable);
  }

  public static Optional<TempBlock> create(@NonNull Block block, @NonNull BlockData data, long duration) {
    return create(block, data, duration, false);
  }

  public static Optional<TempBlock> create(@NonNull Block block, @NonNull BlockData data, long duration, boolean bendable) {
    if (block instanceof TileState) {
      return Optional.empty();
    }

    if (duration <= 0) {
      duration = DEFAULT_REVERT;
    }
    if (duration < 50) {
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

    if (block.getBlockData().matches(data)) {
      return Optional.empty();
    }

    TempBlock tb = MANAGER.get(block).orElse(null);
    if (tb != null && !tb.snapshots.isEmpty()) {
      BlockState state = tb.snapshots.peekFirst().state;
      if (data.matches(state.getBlockData())) {
        tb.revertFully();
        MANAGER.removeEntry(block);
        return Optional.empty();
      }
      tb.addState(data, duration);
      tb.bendable = bendable;
      return Optional.of(tb);
    }

    return Optional.of(new TempBlock(block, bendable).addState(data, duration));
  }

  public static Optional<TempBlock> createAir(@NonNull Block block) {
    return createAir(block, 0);
  }

  public static Optional<TempBlock> createAir(@NonNull Block block, long duration) {
    Material mat = BlockMethods.isInfiniteWater(block) ? Material.WATER : Material.AIR;
    return create(block, mat.createBlockData(), duration, true);
  }

  public static Optional<TempBlock> forceCreateAir(@NonNull Block block) {
    return create(block, Material.AIR.createBlockData(), 0, true);
  }

  private static void refreshGravityCache(Block block) {
    if (block.getType().hasGravity()) {
      GRAVITY_CACHE.add(block);
    } else {
      GRAVITY_CACHE.remove(block);
    }
  }

  private static void invalidateCache(Block block) {
    GRAVITY_CACHE.remove(block);
    TemporalBlockManager.CACHE.remove(block);
  }

  public void setOverwrite() {
    TempBlockState tbs = snapshots.peekLast();
    if (tbs != null) {
      tbs.overwrite = true;
    }
  }

  @Override
  public void revert() {
    TempBlockState tbs = null;
    long time = System.currentTimeMillis();
    while (!snapshots.isEmpty()) {
      tbs = snapshots.pollLast();
      if (time >= tbs.expirationTime) {
        break;
      }
    }
    if (tbs == null) {
      return;
    }
    bendable = tbs.bendable;
    BlockState state = tbs.state;
    state.getWorld().getChunkAtAsync(block).thenRun(() -> state.update(true, false));
    TempBlockState nextState = snapshots.peekLast();
    if (nextState == null) {
      MANAGER.removeEntry(block);
      invalidateCache(block);
    } else {
      refreshGravityCache(block);
      long deltaTime = nextState.expirationTime - time;
      if (deltaTime >= 50) {
        MANAGER.addEntry(block, this, deltaTime);
      }
    }
  }

  private void revertFully() {
    TempBlockState tbs = snapshots.pollFirst();
    if (tbs == null) {
      return;
    }
    block.getWorld().getChunkAtAsync(block).thenRun(() -> tbs.state.update(true, false));
    invalidateCache(block);
  }

  public @NonNull Block getBlock() {
    return block;
  }

  public void removeWithoutReverting() {
    MANAGER.removeEntry(block);
  }

  public boolean isBendable() {
    return bendable;
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

  private static class TempBlockState {
    private final BlockState state;
    private final long expirationTime;
    private final boolean bendable;
    private boolean overwrite = false;

    private TempBlockState(BlockState state, long expirationTime, boolean bendable) {
      this.state = state;
      this.expirationTime = System.currentTimeMillis() + expirationTime;
      this.bendable = bendable;
    }
  }

  private static class TemporalBlockManager extends TemporalManager<Block, TempBlock> {
    private static final Set<Block> CACHE = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isTemp(@Nullable Block key) {
      return CACHE.contains(key);
    }

    @Override
    public void removeAll() {
      getInstances().forEach(TempBlock::revertFully);
      getInstances().clear();
    }
  }
}
