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
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.packet.PacketUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.NonNull;

public class TempArmorStand implements Temporary {
  public static final TemporalManager<TempArmorStand, TempArmorStand> MANAGER = new TemporalManager<>();

  private final int id;
  private boolean reverted = false;

  public static void init() {
  }

  public TempArmorStand(@NonNull World world, @NonNull Vector3d center, @NonNull Material material, long duration, boolean particles) {
    id = PacketUtil.createArmorStand(world, center, material);
    if (particles) {
      Location spawnLoc = center.add(new Vector3d(0, 1.1, 0)).toLocation(world);
      BlockData data = material.createBlockData();
      ParticleUtil.of(Particle.BLOCK_CRACK, spawnLoc).count(4).offset(0.25, 0.125, 0.25)
        .data(data).spawn();
      ParticleUtil.of(Particle.BLOCK_DUST, spawnLoc).count(6).offset(0.25, 0.125, 0.25)
        .data(data).spawn();
    }

    MANAGER.addEntry(this, this, Temporary.toTicks(duration));
  }

  public TempArmorStand(@NonNull World world, @NonNull Vector3d center, @NonNull Material material, long duration) {
    this(world, center, material, duration, true);
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    //armorStand.remove();
    PacketUtil.destroy(id);
    MANAGER.removeEntry(this);
    return true;
  }
}
