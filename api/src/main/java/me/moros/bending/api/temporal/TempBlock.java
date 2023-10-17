/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.api.temporal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.DamageSource;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.property.StateProperty;
import me.moros.bending.api.platform.world.World.Dimension;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import me.moros.math.FastMath;
import me.moros.tasker.TimerWheel;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TempBlock extends Temporary {
  private static final TimerWheel wheel = TimerWheel.hierarchical();
  public static final TemporalManager<Block, TempBlock> MANAGER = new TemporalManager<>(wheel);

  private static final Set<Block> GRAVITY_CACHE = ConcurrentHashMap.newKeySet();

  private final Deque<TempBlockState> snapshots = new ArrayDeque<>();
  private final Block block;
  private Snapshot index;
  private int repeat;
  private boolean reverted = false;

  private TempBlock(Block block, BlockState state, int ticks, Builder builder) {
    this.block = block;
    addState(state, ticks, builder);
    MANAGER.addEntry(block, this, ticks);
  }

  private void addState(BlockState state, int ticks, Builder builder) {
    cleanStates();
    if (index == null || !index.weak) {
      TempBlockState tbs = new TempBlockState(block, ticks, builder);
      snapshots.offerLast(tbs);
      index = tbs;
    } else {
      index.weak = builder.weak;
    }
    block.setState(state);
    refreshGravityCache(block);
  }

  // Cleans up previous states that have already expired
  private void cleanStates() {
    if (snapshots.size() > 1) {
      Iterator<TempBlockState> it = snapshots.iterator();
      it.next(); // ignore original snapshot
      int tick = wheel.currentTick();
      while (it.hasNext()) {
        if (tick > it.next().expirationTicks) {
          it.remove();
        }
      }
    }
  }

  private TempBlockState cleanStatesReverse() {
    TempBlockState toRevert = Objects.requireNonNull(snapshots.pollLast());
    Iterator<TempBlockState> it = snapshots.descendingIterator();
    int tick = wheel.currentTick();
    while (it.hasNext()) {
      TempBlockState next = it.next();
      if (tick >= next.expirationTicks) {
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
    repeat = 0;
    if (MANAGER.clearing()) {
      revertFully();
      return true;
    }
    if (reverted || snapshots.isEmpty()) {
      return false;
    }
    TempBlockState toRevert = cleanStatesReverse();
    if (snapshots.isEmpty()) {
      snapshots.offer(toRevert); // Add original snapshot back
      revertFully();
      return true;
    }
    int t = toRevert.expirationTicks;
    revertToSnapshot(toRevert);
    TempBlockState nextState = snapshots.peekLast();
    if (nextState != null) {
      repeat = Math.max(0, nextState.expirationTicks - t);
    }
    return false;
  }

  @Override
  public int repeat() {
    return repeat;
  }

  public Block block() {
    return block;
  }

  public @Nullable DamageSource damageSource() {
    return index.source;
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

  private void revertToSnapshot(Snapshot snapshot) {
    index = snapshot;
    snapshot.revert();
    refreshGravityCache(block);
  }

  public static void revertToSnapshot(Block block, @Nullable Snapshot snapshot) {
    Consumer<TempBlock> func = snapshot == null ? TempBlock::revert : tb -> tb.revertToSnapshot(snapshot);
    MANAGER.get(block).ifPresentOrElse(func, () -> {
      if (snapshot != null) {
        snapshot.revert();
      }
    });
  }

  private void cleanup() {
    snapshots.clear();
    GRAVITY_CACHE.remove(block);
    MANAGER.removeEntry(block);
  }

  public Snapshot snapshot() {
    return new Snapshot(block, index.bendable, index.weak, index.source);
  }

  public static boolean isBendable(Block block) {
    return MANAGER.get(block).map(tb -> tb.index.bendable).orElse(true);
  }

  public static boolean shouldIgnorePhysics(Block block) {
    return GRAVITY_CACHE.contains(block);
  }

  public static BlockType getLastValidType(Block block) {
    TempBlock tb = MANAGER.get(block).orElse(null);
    if (tb != null && tb.index.weak) {
      return tb.index.state.type();
    }
    return block.type();
  }

  private static void refreshGravityCache(Block block) {
    if (block.type().hasGravity()) {
      GRAVITY_CACHE.add(block);
    } else {
      GRAVITY_CACHE.remove(block);
    }
  }

  private static final class TempBlockState extends Snapshot {
    private final int expirationTicks;

    private TempBlockState(Block block, int ticks, Builder builder) {
      super(block, builder.bendable, builder.weak, builder.source);
      this.expirationTicks = wheel.currentTick() + ticks;
    }
  }

  public static class Snapshot {
    protected final DamageSource source;
    protected final Block block;
    protected final BlockState state;
    protected final boolean bendable;
    protected boolean weak;

    private Snapshot(Block block, boolean bendable, boolean weak, @Nullable DamageSource source) {
      this.block = block;
      this.state = block.state();
      this.bendable = bendable;
      this.weak = weak;
      this.source = source;
    }

    private void revert() {
      block.world().loadChunkAsync(block).thenRun(() -> block.setState(state));
    }
  }

  public static Builder builder(BlockType type) {
    return builder(type.defaultState());
  }

  public static Builder builder(BlockState state) {
    return new Builder(Objects.requireNonNull(state));
  }

  /**
   * @return a {@link Builder} with bendable fire
   */
  public static Builder fire() {
    return builder(BlockType.FIRE).bendable(true);
  }

  /**
   * @return a {@link Builder} with water
   */
  public static Builder water() {
    return builder(BlockType.WATER);
  }

  /**
   * @return a {@link Builder} with bendable ice
   */
  public static Builder ice() {
    return builder(BlockType.ICE).bendable(true);
  }

  /**
   * @return a {@link Builder} with bendable air
   */
  public static Builder air() {
    return builder(BlockType.AIR).bendable(true);
  }

  public static final class Builder {
    private final BlockState state;

    private DamageSource source;
    private boolean fixWater;
    private boolean bendable = false;
    private boolean weak = false;
    private long duration = 0;

    private Builder(BlockState state) {
      this.state = state;
      fixWater = state.type().isAir();
    }

    public Builder fixWater(boolean fixWater) {
      this.fixWater = fixWater;
      return this;
    }

    public Builder bendable(boolean bendable) {
      this.bendable = bendable;
      return this;
    }

    public Builder weak(boolean weak) {
      this.weak = weak;
      return this;
    }

    public Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Builder ability(@Nullable Ability ability) {
      this.source = ability == null ? null : DamageSource.of(ability.user().name(), ability.description());
      return this;
    }

    private int validateDuration(Block block) {
      long time = duration;
      if (WaterMaterials.ICE_BENDABLE.isTagged(state) && block.world().dimension() == Dimension.NETHER) {
        time = FastMath.floor(0.5 * time);
      }
      return MANAGER.fromMillis(time);
    }

    // Handle falling water
    private BlockState calculateWaterData(Block above) {
      int level;
      var property = above.state().property(StateProperty.LEVEL);
      if (property != null) {
        level = property;
        if (level <= 7) {
          level += 8;
        }
      } else {
        level = 8;
      }
      return MaterialUtil.waterData(level);
    }

    private BlockState correctData(Block block) {
      BlockState newData = state;
      boolean infiniteWater = false;
      if (fixWater) {
        infiniteWater = WorldUtil.isInfiniteWater(block);
        BlockType mat = infiniteWater ? BlockType.WATER : BlockType.AIR;
        Block above = block.offset(Direction.UP);
        if (mat == BlockType.AIR && MaterialUtil.isWater(above)) {
          newData = calculateWaterData(above);
        } else {
          newData = mat.defaultState();
        }
      }
      BlockState old = block.state();
      var waterlogged = old.property(StateProperty.WATERLOGGED);
      if (waterlogged != null) {
        if (waterlogged && newData.type().isAir()) {
          return old.withProperty(StateProperty.WATERLOGGED, false);
        } else if (infiniteWater || (!waterlogged && newData.type() == BlockType.WATER)) {
          return old.withProperty(StateProperty.WATERLOGGED, true);
        }
      }
      return newData;
    }

    public Optional<TempBlock> build(Block block) {
      if (block.world().isTileEntity(block)) {
        return Optional.empty();
      }
      BlockState current = block.state();
      BlockState newData = correctData(block);
      if (current.matches(newData)) {
        return Optional.empty();
      }
      int ticks = validateDuration(block);
      TempBlock tb = MANAGER.get(block).orElse(null);
      if (tb != null) {
        TempBlockState first = tb.snapshots.peekFirst();
        if (first != null && newData.matches(first.state)) {
          tb.revertFully();
        }
        if (tb.reverted || tb.snapshots.isEmpty()) {
          MANAGER.removeEntry(block);
          return Optional.empty();
        }
        tb.addState(newData, ticks, this);
        wheel.schedule(tb, ticks);
        return Optional.of(tb);
      }
      TempBlock result = new TempBlock(block, newData, ticks, this);
      return Optional.of(result);
    }
  }
}
