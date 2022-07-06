/*
 * Copyright 2020-2022 Moros
 *
 * This file is part of Bending.
 *
 * Bending is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Bending is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bending. If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.game.temporal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.adapter.impl.NativeAdapter;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TempBlock extends TemporaryBase {
  public static final TemporalManager<Block, TempBlock> MANAGER = new TemporalManager<>("Block", TempBlock::revertFully);

  private static final Set<Block> GRAVITY_CACHE = ConcurrentHashMap.newKeySet();

  private final Deque<TempBlockState> snapshots;
  private final Block block;
  private boolean bendable;
  private boolean reverted = false;

  private TempBlock(Block block, boolean bendable, int ticks) {
    super();
    snapshots = new ArrayDeque<>();
    this.block = block;
    this.bendable = bendable;
    snapshots.offerLast(new TempBlockState(block.getState(), bendable, ticks));
  }

  // Handle falling water
  private static BlockData calculateWaterData(Block above) {
    BlockData data = above.getBlockData();
    int level;
    if (data instanceof Levelled levelled) {
      level = levelled.getLevel();
      if (level <= 7) {
        level += 8;
      }
    } else {
      level = 8;
    }
    return MaterialUtil.waterData(level);
  }

  private void addState(BlockData data, boolean bendable, int ticks) {
    cleanStates();
    if (!snapshots.isEmpty()) {
      TempBlockState tbs = snapshots.peekLast();
      if (!tbs.weak) {
        snapshots.offerLast(new TempBlockState(block.getState(false), bendable, ticks));
      } else {
        tbs.weak = false;
      }
      this.bendable = bendable;
      NativeAdapter.instance().setBlockFast(block, data);
      refreshGravityCache(block);
      MANAGER.reschedule(block, ticks);
    }
  }

  // Cleans up previous states that have already expired
  private void cleanStates() {
    if (snapshots.size() > 1) {
      int currentTick = Bukkit.getCurrentTick();
      Iterator<TempBlockState> it = snapshots.iterator();
      it.next(); // ignore original snapshot
      while (it.hasNext()) {
        if (currentTick > it.next().expirationTicks) {
          it.remove();
        }
      }
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
  public boolean revert() {
    if (reverted || snapshots.isEmpty()) {
      return false;
    }
    TempBlockState toRevert = cleanStatesReverse();
    if (snapshots.isEmpty()) {
      snapshots.offer(toRevert); // Add original snapshot back
      revertFully();
      return true;
    }
    revertToSnapshot(toRevert);
    TempBlockState nextState = snapshots.peekLast();
    if (nextState != null) {
      int deltaTicks = nextState.expirationTicks - Bukkit.getCurrentTick();
      if (deltaTicks > 0) {
        MANAGER.reschedule(block, deltaTicks);
      }
    }
    return false;
  }

  public @NonNull Block block() {
    return block;
  }

  public void forceWeak() {
    TempBlockState tbs = snapshots.peekLast();
    if (tbs != null) {
      tbs.weak = true;
    }
  }

  public void removeWithoutReverting() {
    cleanup();
  }

  private void revertFully() {
    if (reverted || snapshots.isEmpty()) {
      reverted = true;
      return;
    }
    revertToSnapshot(snapshots.pollFirst());
    cleanup();
    reverted = true;
  }

  private void revertToSnapshot(@NonNull Snapshot snapshot) {
    bendable = snapshot.bendable;
    BlockState state = snapshot.state;
    block.getWorld().getChunkAtAsync(block).thenRun(() -> state.update(true, false));
    refreshGravityCache(block);
  }

  public static void revertToSnapshot(@NonNull Block block, @Nullable Snapshot snapshot) {
    TempBlock tb = MANAGER.get(block).orElse(null);
    if (snapshot == null) {
      if (tb != null) {
        tb.revert();
      }
      return;
    }
    if (tb != null) {
      tb.revertToSnapshot(snapshot);
    } else {
      block.getWorld().getChunkAtAsync(block).thenRun(() -> snapshot.state.update(true, false));
    }
  }

  private void cleanup() {
    snapshots.clear();
    GRAVITY_CACHE.remove(block);
    MANAGER.removeEntry(block);
  }

  public @NonNull Snapshot snapshot() {
    return new Snapshot(block.getState(), bendable);
  }

  public static boolean isBendable(@NonNull Block block) {
    return MANAGER.get(block).map(tb -> tb.bendable).orElse(true);
  }

  public static boolean shouldIgnorePhysics(@NonNull Block block) {
    return GRAVITY_CACHE.contains(block);
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

  private static void refreshGravityCache(Block block) {
    if (block.getType().hasGravity()) {
      GRAVITY_CACHE.add(block);
    } else {
      GRAVITY_CACHE.remove(block);
    }
  }

  private static final class TempBlockState extends Snapshot {
    private final int expirationTicks;
    private boolean weak = false;

    private TempBlockState(BlockState state, boolean bendable, int ticks) {
      super(state, bendable);
      this.expirationTicks = Bukkit.getCurrentTick() + ticks;
    }
  }

  public static class Snapshot {
    protected final BlockState state;
    protected final boolean bendable;

    private Snapshot(BlockState state, boolean bendable) {
      this.state = state;
      this.bendable = bendable;
    }
  }

  public static @NonNull Builder builder(@NonNull BlockData data) {
    return new Builder(Objects.requireNonNull(data));
  }

  /**
   * @return a {@link Builder} with bendable fire
   */
  public static @NonNull Builder fire() {
    return builder(Material.FIRE.createBlockData()).bendable(true);
  }

  /**
   * @return a {@link Builder} with water
   */
  public static @NonNull Builder water() {
    return builder(Material.WATER.createBlockData());
  }

  /**
   * @return a {@link Builder} with bendable ice
   */
  public static @NonNull Builder ice() {
    return builder(Material.ICE.createBlockData()).bendable(true);
  }

  /**
   * @return a {@link Builder} with bendable air
   */
  public static @NonNull Builder air() {
    return builder(Material.AIR.createBlockData()).bendable(true);
  }

  public static final class Builder {
    private final BlockData data;

    private boolean fixWater;
    private boolean bendable = false;
    private long duration = 0;

    private Builder(BlockData data) {
      this.data = data;
      fixWater = data.getMaterial().isAir();
    }

    public @NonNull Builder fixWater(boolean fixWater) {
      this.fixWater = fixWater;
      return this;
    }

    public @NonNull Builder bendable(boolean bendable) {
      this.bendable = bendable;
      return this;
    }

    public @NonNull Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    private int validateDuration(Block block) {
      long time = duration <= 0 ? DEFAULT_REVERT : duration;
      if (WaterMaterials.ICE_BENDABLE.isTagged(data) && block.getWorld().getEnvironment() == Environment.NETHER) {
        time = FastMath.floor(0.5 * time);
      }
      return Temporary.toTicks(time);
    }

    private BlockData fixWaterData(Block block) {
      if (fixWater) {
        Material mat = WorldUtil.isInfiniteWater(block) ? Material.WATER : Material.AIR;
        Block above = block.getRelative(BlockFace.UP);
        return mat == Material.AIR && MaterialUtil.isWater(above) ? calculateWaterData(above) : mat.createBlockData();
      }
      return data;
    }

    private BlockData validateWaterlogged(BlockData blockData, BlockData newData) {
      if (blockData instanceof Waterlogged waterData) {
        if (waterData.isWaterlogged() && newData.getMaterial().isAir()) {
          waterData.setWaterlogged(false);
        } else if (!waterData.isWaterlogged() && newData.getMaterial() == Material.WATER) {
          waterData.setWaterlogged(true);
        }
        return waterData;
      }
      return newData;
    }

    public Optional<TempBlock> build(@NonNull Block block) {
      if (block instanceof TileState) {
        return Optional.empty();
      }
      BlockData blockData = block.getBlockData();
      BlockData newData = validateWaterlogged(blockData, fixWaterData(block));
      if (blockData.matches(newData)) {
        return Optional.empty();
      }
      int ticks = validateDuration(block);
      TempBlock tb = MANAGER.get(block).orElse(null);
      if (tb != null && !tb.snapshots.isEmpty()) {
        if (newData.matches(tb.snapshots.peekFirst().state.getBlockData())) {
          tb.revertFully();
          MANAGER.removeEntry(block);
          return Optional.empty();
        }
        tb.addState(newData, bendable, ticks);
        return Optional.of(tb);
      }
      TempBlock result = new TempBlock(block, bendable, ticks);
      if (NativeAdapter.instance().setBlockFast(block, newData)) {
        refreshGravityCache(block);
        MANAGER.addEntry(block, result, ticks);
        return Optional.of(result);
      }
      return Optional.empty();
    }
  }
}
