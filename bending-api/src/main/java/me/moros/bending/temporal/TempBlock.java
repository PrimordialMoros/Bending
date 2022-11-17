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

package me.moros.bending.temporal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.event.AbilityEvent;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import me.moros.bending.model.user.User;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class TempBlock extends TemporaryBase {
  public static final TemporalManager<Block, TempBlock> MANAGER = new TemporalManager<>("Block", TempBlock::revertFully);

  private static final Set<Block> GRAVITY_CACHE = ConcurrentHashMap.newKeySet();

  private final Deque<TempBlockState> snapshots = new ArrayDeque<>();
  private final Block block;
  private Snapshot index;
  private boolean reverted = false;

  private TempBlock(Block block, BlockData data, int ticks, Builder builder) {
    super();
    this.block = block;
    addState(data, ticks, builder);
    MANAGER.addEntry(block, this, ticks);
  }

  private void addState(BlockData data, int ticks, Builder builder) {
    cleanStates();
    if (index == null || !index.weak) {
      TempBlockState tbs = new TempBlockState(block, ticks, builder);
      snapshots.offerLast(tbs);
      index = tbs;
    } else {
      index.weak = builder.weak;
    }
    NativeAdapter.instance().setBlockFast(block, data);
    refreshGravityCache(block);
  }

  // Cleans up previous states that have already expired
  private void cleanStates() {
    if (snapshots.size() > 1) {
      Iterator<TempBlockState> it = snapshots.iterator();
      it.next(); // ignore original snapshot
      while (it.hasNext()) {
        if (MANAGER.currentTick() > it.next().expirationTicks) {
          it.remove();
        }
      }
    }
  }

  private TempBlockState cleanStatesReverse() {
    TempBlockState toRevert = Objects.requireNonNull(snapshots.pollLast());
    Iterator<TempBlockState> it = snapshots.descendingIterator();
    while (it.hasNext()) {
      TempBlockState next = it.next();
      if (MANAGER.currentTick() >= next.expirationTicks) {
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
      int deltaTicks = nextState.expirationTicks - MANAGER.currentTick();
      if (deltaTicks > 0) {
        MANAGER.reschedule(block, deltaTicks);
      }
    }
    return false;
  }

  public Block block() {
    return block;
  }

  public @Nullable AbilityEvent damageSource() {
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
      snapshot.revert();
    }
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

  public static BlockData getLastValidData(Block block) {
    TempBlock tb = MANAGER.get(block).orElse(null);
    if (tb != null && tb.index.weak) {
      return tb.index.state.getBlockData();
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

    private TempBlockState(Block block, int ticks, Builder builder) {
      super(block, builder.bendable, builder.weak, builder.source);
      this.expirationTicks = MANAGER.currentTick() + ticks;
    }
  }

  private record DamageSource(User user, AbilityDescription ability) implements AbilityEvent {
  }

  public static class Snapshot {
    protected final DamageSource source;
    protected final BlockState state;
    protected final boolean bendable;
    protected boolean weak;

    private Snapshot(Block block, boolean bendable, boolean weak, @Nullable DamageSource source) {
      this.state = block.getState(false);
      this.bendable = bendable;
      this.weak = weak;
      this.source = source;
    }

    private void revert() {
      Block block = state.getBlock();
      state.getWorld().getChunkAtAsync(block).thenRun(() -> NativeAdapter.instance().setBlockFast(block, state.getBlockData()));
    }
  }

  public static Builder builder(BlockData data) {
    return new Builder(Objects.requireNonNull(data));
  }

  /**
   * @return a {@link Builder} with bendable fire
   */
  public static Builder fire() {
    return builder(Material.FIRE.createBlockData()).bendable(true);
  }

  /**
   * @return a {@link Builder} with water
   */
  public static Builder water() {
    return builder(Material.WATER.createBlockData());
  }

  /**
   * @return a {@link Builder} with bendable ice
   */
  public static Builder ice() {
    return builder(Material.ICE.createBlockData()).bendable(true);
  }

  /**
   * @return a {@link Builder} with bendable air
   */
  public static Builder air() {
    return builder(Material.AIR.createBlockData()).bendable(true);
  }

  public static final class Builder {
    private final BlockData data;

    private DamageSource source;
    private boolean fixWater;
    private boolean bendable = false;
    private boolean weak = false;
    private long duration = 0;

    private Builder(BlockData data) {
      this.data = data;
      fixWater = data.getMaterial().isAir();
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
      this.source = ability == null ? null : new DamageSource(ability.user(), ability.description());
      return this;
    }

    private int validateDuration(Block block) {
      long time = duration <= 0 ? DEFAULT_REVERT : duration;
      if (WaterMaterials.ICE_BENDABLE.isTagged(data) && block.getWorld().getEnvironment() == Environment.NETHER) {
        time = FastMath.floor(0.5 * time);
      }
      return Temporary.toTicks(time);
    }

    // Handle falling water
    private BlockData calculateWaterData(Block above) {
      int level;
      if (above.getBlockData() instanceof Levelled levelled) {
        level = levelled.getLevel();
        if (level <= 7) {
          level += 8;
        }
      } else {
        level = 8;
      }
      return MaterialUtil.waterData(level);
    }

    private BlockData correctData(Block block) {
      BlockData newData = data;
      if (fixWater) {
        Material mat = WorldUtil.isInfiniteWater(block) ? Material.WATER : Material.AIR;
        Block above = block.getRelative(BlockFace.UP);
        if (mat == Material.AIR && MaterialUtil.isWater(above)) {
          newData = calculateWaterData(above);
        } else {
          newData = mat.createBlockData();
        }
      }
      if (block.getBlockData() instanceof Waterlogged waterData) {
        if (waterData.isWaterlogged() && newData.getMaterial().isAir()) {
          waterData.setWaterlogged(false);
          return waterData;
        } else if (!waterData.isWaterlogged() && newData.getMaterial() == Material.WATER) {
          waterData.setWaterlogged(true);
          return waterData;
        }
      }
      return newData;
    }

    public Optional<TempBlock> build(Block block) {
      if (block.getState(false) instanceof TileState) {
        return Optional.empty();
      }
      BlockData current = block.getBlockData();
      BlockData newData = correctData(block);
      if (current.matches(newData)) {
        return Optional.empty();
      }
      int ticks = validateDuration(block);
      TempBlock tb = MANAGER.get(block).orElse(null);
      if (tb != null) {
        TempBlockState first = tb.snapshots.peekFirst();
        if (first != null && newData.matches(first.state.getBlockData())) {
          tb.revertFully();
        }
        if (tb.reverted || tb.snapshots.isEmpty()) {
          MANAGER.removeEntry(block);
          return Optional.empty();
        }
        tb.addState(newData, ticks, this);
        MANAGER.reschedule(block, ticks);
        return Optional.of(tb);
      }
      TempBlock result = new TempBlock(block, newData, ticks, this);
      return Optional.of(result);
    }
  }
}
