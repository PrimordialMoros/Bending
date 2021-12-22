/*
 * Copyright 2020-2021 Moros
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

import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TempFallingBlock implements Temporary {
  public static final TemporalManager<FallingBlock, TempFallingBlock> MANAGER = new TemporalManager<>();

  private final FallingBlock fallingBlock;
  private boolean reverted = false;

  public static void init() {
  }

  public TempFallingBlock(@NonNull Location location, @NonNull BlockData data, @NonNull Vector3d velocity, boolean gravity, long duration) {
    fallingBlock = location.getWorld().spawnFallingBlock(location, data);
    fallingBlock.setVelocity(velocity.clampVelocity());
    fallingBlock.setGravity(gravity);
    fallingBlock.setDropItem(false);
    MANAGER.addEntry(fallingBlock, this, Temporary.toTicks(duration));
  }

  public TempFallingBlock(@NonNull Location location, @NonNull BlockData data, long duration) {
    this(location, data, Vector3d.ZERO, false, duration);
  }

  public TempFallingBlock(@NonNull Block block, @NonNull BlockData data, @NonNull Vector3d velocity, boolean gravity, long duration) {
    this(block.getLocation().add(0.5, 0, 0.5), data, velocity, gravity, duration);
  }

  public TempFallingBlock(@NonNull Block block, @NonNull BlockData data, long duration) {
    this(block, data, Vector3d.ZERO, false, duration);
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
}
