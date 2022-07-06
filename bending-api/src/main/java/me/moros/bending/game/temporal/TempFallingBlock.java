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

import java.util.Objects;

import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class TempFallingBlock extends TemporaryBase {
  public static final TemporalManager<FallingBlock, TempFallingBlock> MANAGER = new TemporalManager<>("Falling Block");
  public static final Vector3d OFFSET = new Vector3d(0.5, 0, 0.5);

  private final FallingBlock fallingBlock;
  private boolean reverted = false;

  private TempFallingBlock(FallingBlock fallingBlock, long duration) {
    super();
    this.fallingBlock = fallingBlock;
    MANAGER.addEntry(fallingBlock, this, Temporary.toTicks(duration));
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    fallingBlock.remove();
    MANAGER.removeEntry(fallingBlock);
    return true;
  }

  public boolean valid() {
    return !reverted;
  }

  public @NonNull FallingBlock fallingBlock() {
    return fallingBlock;
  }

  public @NonNull Vector3d center() {
    return new Vector3d(fallingBlock.getLocation()).add(new Vector3d(0, 0.5, 0));
  }

  public static @NonNull Builder builder(@NonNull BlockData data) {
    return new Builder(Objects.requireNonNull(data));
  }

  public static final class Builder {
    private final BlockData data;

    private Vector3d velocity = Vector3d.ZERO;
    private boolean gravity = true;
    private long duration = 30000;

    private Builder(BlockData data) {
      this.data = data;
    }

    public @NonNull Builder velocity(@NonNull Vector3d velocity) {
      this.velocity = Objects.requireNonNull(velocity);
      return this;
    }

    public @NonNull Builder gravity(boolean gravity) {
      this.gravity = gravity;
      return this;
    }

    public @NonNull Builder duration(long duration) {
      this.duration = duration;
      return this;
    }

    public @NonNull TempFallingBlock build(@NonNull Block block) {
      return build(block.getWorld(), new Vector3d(block).add(OFFSET));
    }

    public @NonNull TempFallingBlock build(@NonNull World world, @NonNull Vector3d center) {
      FallingBlock fallingBlock = Objects.requireNonNull(world).spawnFallingBlock(Objects.requireNonNull(center).toLocation(world), data);
      fallingBlock.setVelocity(velocity.clampVelocity());
      fallingBlock.setGravity(gravity);
      fallingBlock.setDropItem(false);
      return new TempFallingBlock(fallingBlock, duration);
    }
  }
}
