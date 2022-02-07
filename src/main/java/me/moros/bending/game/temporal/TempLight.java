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

import java.util.Optional;

import me.moros.bending.config.Configurable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import me.moros.bending.util.internal.PacketUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class TempLight extends TemporaryBase {
  private static final int MAX_LEVEL;
  private static final BlockData[] LIGHT_ARRAY;

  static {
    Levelled data = (Levelled) Material.LIGHT.createBlockData();
    MAX_LEVEL = data.getMaximumLevel();
    LIGHT_ARRAY = new BlockData[MAX_LEVEL - 1];
    Levelled clone;
    for (int i = 0; i < MAX_LEVEL - 1; i++) {
      clone = (Levelled) data.clone();
      clone.setLevel(i + 1);
      LIGHT_ARRAY[i] = clone;
    }
  }

  public static final TemporalManager<Block, TempLight> MANAGER = new TemporalManager<>("Light");

  private final Block block;
  private final Vector3d pos;
  private int level;
  private final int rate;
  private boolean lock = false;
  private boolean reverted = false;

  public static void init() {
  }

  private TempLight(Block block, int level, int rate, long duration) {
    super();
    this.block = block;
    this.pos = new Vector3d(block);
    this.level = level;
    this.rate = rate;
    MANAGER.addEntry(block, this, Temporary.toTicks(duration));
  }

  @Override
  public boolean revert() {
    if (reverted || lock) {
      return false;
    }
    level -= rate;
    Type type = isValid(block, level);
    if (level <= 0 || type == Type.INVALID) {
      reverted = true;
      PacketUtil.fakeBlock(block.getWorld(), pos, block.getBlockData());
      MANAGER.removeEntry(block);
      return true;
    }
    render(type == Type.WATER);
    MANAGER.reschedule(block, 2);
    return false;
  }

  private void render(boolean waterlogged) {
    BlockData data = LIGHT_ARRAY[level - 1];
    if (waterlogged) {
      data = data.clone();
      ((Waterlogged) data).setWaterlogged(true);
    }
    PacketUtil.fakeBlock(block.getWorld(), pos, data);
  }

  public @NonNull TempLight lock() {
    lock = true;
    return this;
  }

  public @NonNull TempLight unlockAndRevert() {
    lock = false;
    revert();
    return this;
  }

  public @NonNull Block block() {
    return block;
  }

  public int level() {
    return level;
  }

  private static Type isValid(Block block, int level) {
    if (block.getLightLevel() < level) {
      BlockData data = block.getBlockData();
      Material mat = data.getMaterial();
      if (mat.isAir()) {
        return Type.NORMAL;
      } else if (mat == Material.WATER && ((Levelled) data).getLevel() == 0) {
        return Type.WATER;
      }
    }
    return Type.INVALID;
  }

  public static @NonNull Builder builder(int level) {
    return new Builder(Math.max(1, Math.min(LIGHT_ARRAY.length, level)));
  }

  private enum Type {NORMAL, WATER, INVALID}

  public static final class Builder {
    private final int level;

    private int rate = 3;
    private long duration = 100;

    private Builder(int level) {
      this.level = level;
    }

    public @NonNull Builder rate(int rate) {
      this.rate = Math.max(1, rate);
      return this;
    }

    public @NonNull Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public Optional<TempLight> build(@NonNull Block block) {
      if (!config.enabled) {
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
          light.render(type == Type.WATER);
        }
        return Optional.of(light);
      }
      light = new TempLight(block, level, rate, duration);
      light.render(type == Type.WATER);
      return Optional.of(light);
    }
  }

  private static final Config config = new Config();

  private static class Config extends Configurable {
    private boolean enabled;

    @Override
    public void onConfigReload() {
      enabled = config.node("properties", "generate-light").getBoolean(true);
    }
  }
}
