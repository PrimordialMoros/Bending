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
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.scheduler.BukkitTask;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TempBlock implements Temporary {
  private static final Set<Block> GRAVITY_CACHE = ConcurrentHashMap.newKeySet();

  public static final TemporalManager<Block, TempBlock> MANAGER = new TempBlockManager();

  private final Deque<TempBlockState> snapshots;
  private final Block block;
  private BukkitTask revertTask;
  private boolean bendable;

  public static void init() {
  }

  private TempBlock(Block block, BlockData data, long duration, boolean bendable) {
    snapshots = new ArrayDeque<>();
    this.block = block;
    this.bendable = bendable;
    snapshots.offerLast(new TempBlockState(block.getState(), duration, bendable));
    block.setBlockData(data);
    refreshGravityCache(block);
    MANAGER.addEntry(block, this);
    revertTask = Tasker.simpleTask(this::revert, Temporary.toTicks(duration));
  }

  private void addState(BlockData data, long duration, boolean bendable) {
    cleanStates();
    if (!snapshots.isEmpty()) {
      TempBlockState tbs = snapshots.peekLast();
      if (!tbs.weak) {
        snapshots.offerLast(new TempBlockState(block.getState(false), duration, bendable));
      } else {
        tbs.weak = false;
      }
      this.bendable = bendable;
      block.setBlockData(data);
      refreshGravityCache(block);
      revertTask.cancel();
      revertTask = Tasker.simpleTask(this::revert, Temporary.toTicks(duration));
    }
  }

  // Cleans up previous states that have already expired
  private void cleanStates() {
    if (snapshots.size() <= 1) {
      return;
    }
    int currentTick = Bukkit.getCurrentTick();
    Iterator<TempBlockState> it = snapshots.iterator();
    it.next(); // ignore original snapshot
    while (it.hasNext()) {
      TempBlockState tbs = it.next();
      if (currentTick > tbs.expirationTicks) {
        it.remove();
      }
    }
  }

  public static @NonNull BlockData getLastValidData(@NonNull Block block) {
    TempBlock tb = MANAGER.get(block).orElse(null);
    if (tb != null) {
      TempBlockState tbs = tb.snapshots.peekLast();
      if (tbs != null && tbs.weak) {
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
      tb.addState(data, duration, bendable);
      return Optional.of(tb);
    }

    return Optional.of(new TempBlock(block, data, duration, bendable));
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

  public void forceWeak() {
    TempBlockState tbs = snapshots.peekLast();
    if (tbs != null) {
      tbs.weak = true;
    }
  }

  private TempBlockState cleanStatesReverse() {
    int currentTick = Bukkit.getCurrentTick();
    TempBlockState toRevert = snapshots.pollLast();
    Iterator<TempBlockState> it = snapshots.descendingIterator();
    while (it.hasNext()) {
      TempBlockState next = it.next();
      if (currentTick >= next.expirationTicks) {
        it.remove();
        toRevert = next;
      } else {
        break;
      }
    }
    return toRevert;
  }

  @Override
  public void revert() {
    if (revertTask.isCancelled() || snapshots.isEmpty()) {
      return;
    }
    revertTask.cancel();
    TempBlockState toRevert = cleanStatesReverse();
    if (snapshots.isEmpty()) {
      snapshots.offer(toRevert); // Add original snapshot back
      revertFully();
      return;
    }
    revertToSnapshot(toRevert);
    TempBlockState nextState = snapshots.peekLast();
    if (nextState != null) {
      int deltaTicks = nextState.expirationTicks - Bukkit.getCurrentTick();
      if (deltaTicks > 0) {
        revertTask = Tasker.simpleTask(this::revert, deltaTicks);
      }
    }
  }

  private void revertToSnapshot(final TempBlockState tempBlockState) {
    bendable = tempBlockState.bendable;
    BlockState state = tempBlockState.state;
    block.getWorld().getChunkAtAsync(block).thenRun(() -> state.update(true, false));
    refreshGravityCache(block);
  }

  private void revertFully() {
    if (snapshots.isEmpty()) {
      return;
    }
    revertToSnapshot(snapshots.pollFirst());
    snapshots.clear();
    GRAVITY_CACHE.remove(block);
    MANAGER.removeEntry(block);
    revertTask.cancel();
  }

  public @NonNull Block block() {
    return block;
  }

  public void removeWithoutReverting() {
    GRAVITY_CACHE.remove(block);
    MANAGER.removeEntry(block);
    revertTask.cancel();
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

  private static class TempBlockManager extends TemporalManager<Block, TempBlock> {
    @Override
    public void removeAll() {
      new ArrayList<>(instances().values()).forEach(TempBlock::revertFully);
      clear();
    }
  }

  private static class TempBlockState {
    private final BlockState state;
    private final int expirationTicks;
    private final boolean bendable;
    private boolean weak = false;

    private TempBlockState(BlockState state, long expirationTime, boolean bendable) {
      this.state = state;
      this.expirationTicks = Bukkit.getCurrentTick() + Temporary.toTicks(expirationTime);
      this.bendable = bendable;
    }
  }
}
