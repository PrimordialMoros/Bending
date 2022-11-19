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

package me.moros.bending.model.ability.common;

import me.moros.bending.model.ability.Updatable;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;

public class EarthSpike implements Updatable {
  private static final long DELAY = 80;
  private static final long DURATION = 15_000;

  private final Block origin;

  private final int length;

  private int currentLength = 0;
  private long nextUpdateTime;

  public EarthSpike(Block origin, int length, boolean delay) {
    this.origin = origin;
    this.length = length;
    nextUpdateTime = delay ? System.currentTimeMillis() + DELAY : 0;
  }

  @Override
  public UpdateResult update() {
    if (currentLength >= length) {
      return UpdateResult.REMOVE;
    }
    long time = System.currentTimeMillis();
    if (time < nextUpdateTime) {
      return UpdateResult.CONTINUE;
    }
    if (currentLength == 0) {
      if (!EarthMaterials.isEarthOrSand(origin)) {
        return UpdateResult.REMOVE;
      }
      BlockData data = MaterialUtil.solidType(origin.getBlockData(), Material.DRIPSTONE_BLOCK.createBlockData());
      TempBlock.builder(data).duration(DURATION).build(origin);
    }
    nextUpdateTime = time + DELAY;
    Block currentIndex = origin.getRelative(BlockFace.UP, ++currentLength);
    if (canMove(currentIndex)) {
      Vector3d center = Vector3d.fromCenter(currentIndex);
      ParticleUtil.of(Particle.BLOCK_DUST, center).count(24).offset(0.2)
        .data(Material.DRIPSTONE_BLOCK.createBlockData()).spawn(currentIndex.getWorld());
      TempBlock.builder(Material.POINTED_DRIPSTONE.createBlockData()).duration(DURATION - currentLength * DELAY).build(currentIndex);
      SoundUtil.EARTH.play(currentIndex);
    } else {
      return UpdateResult.REMOVE;
    }
    return UpdateResult.CONTINUE;
  }

  private boolean canMove(Block newBlock) {
    if (MaterialUtil.isLava(newBlock)) {
      return false;
    }
    if (!MaterialUtil.isTransparent(newBlock)) {
      return false;
    }
    WorldUtil.tryBreakPlant(newBlock);
    return true;
  }
}
