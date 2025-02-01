/*
 * Copyright 2020-2025 Moros
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

import java.util.List;
import java.util.Optional;

import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockStateProperties;
import me.moros.bending.api.platform.block.BlockType;

public final class TempLight extends Temporary {
  public static final TemporalManager<Block, TempLight> MANAGER = new TemporalManager<>(600) {
    @Override
    public void tick() {
      List<TempLight> toRemove = MANAGER.stream().filter(TempLight::tick).toList();
      toRemove.forEach(TempLight::revertFully);
      super.tick();
    }
  };

  private final Block block;
  private int level;
  private final int rate;
  private boolean lock = false;
  private boolean reverted = false;
  private Type lastType;
  private boolean skipTick = false;

  private TempLight(Block block, int level, int rate, int ticks, Type type) {
    this.block = block;
    this.level = level;
    this.rate = rate;
    this.lastType = type;
    render();
    MANAGER.addEntry(block, this, ticks);
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    revertFully();
    return true;
  }

  private void revertFully() {
    if (!reverted) {
      Platform.instance().nativeAdapter().fakeBlock(block, block.state()).broadcast(block.world(), block);
      MANAGER.removeEntry(block);
      reverted = true;
    }
  }

  private boolean tick() {
    skipTick ^= true;
    if (lock || reverted || skipTick) {
      return false;
    }
    level -= rate;
    if (level > 0) {
      lastType = isValid(block, level);
      if (lastType != Type.INVALID) {
        render();
        return false;
      }
    }
    return true;
  }

  private void render() {
    boolean waterlogged = lastType == Type.WATER;
    BlockState state = BlockType.LIGHT.defaultState().withProperty(BlockStateProperties.LEVEL, level);
    if (waterlogged) {
      state = state.withProperty(BlockStateProperties.WATERLOGGED, true);
    }
    Platform.instance().nativeAdapter().fakeBlock(block, state).broadcast(block.world(), block);
  }

  public TempLight lock() {
    lock = true;
    return this;
  }

  public void unlock() {
    lock = false;
  }

  public Block block() {
    return block;
  }

  public int level() {
    return level;
  }

  private static Type isValid(Block block, int level) {
    if (block.world().lightLevel(block) < level) {
      BlockState state = block.state();
      BlockType mat = state.type();
      if (mat.isAir()) {
        return Type.NORMAL;
      } else if (mat == BlockType.WATER) {
        var property = state.property(BlockStateProperties.LEVEL);
        if (property != null && property == 0) {
          return Type.WATER;
        }
      }
    }
    return Type.INVALID;
  }

  public static Builder builder(int level) {
    return new Builder(Math.clamp(level, 1, BlockStateProperties.LEVEL.max()));
  }

  private enum Type {NORMAL, WATER, INVALID}

  public static final class Builder {
    private final int level;

    private int rate = 3;
    private long duration = 750;

    private Builder(int level) {
      this.level = level;
    }

    public Builder rate(int rate) {
      this.rate = Math.max(1, rate);
      return this;
    }

    public Builder duration(long duration) {
      this.duration = (duration <= 0 || duration > 30_000) ? 30_000 : duration;
      return this;
    }

    public Optional<TempLight> build(Block block) {
      if (!BendingProperties.instance().canGenerateLight()) {
        return Optional.empty();
      }
      Type type = isValid(block, level);
      if (type == Type.INVALID) {
        return Optional.empty();
      }
      TempLight light = MANAGER.get(block).orElse(null);
      if (light != null) {
        if (light.level < level) {
          light.level = level;
          light.lastType = type;
          light.render();
        }
        return Optional.of(light);
      }
      return Optional.of(new TempLight(block, level, rate, MANAGER.fromMillis(duration), type));
    }
  }
}
