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

package me.moros.bending.ability.earth.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempFallingBlock;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.packet.PacketUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Fracture implements Updatable {
  private final List<Block> wall;
  private final Map<Block, Integer> wallData;

  private Fracture(List<Block> wall) {
    this.wall = new ArrayList<>(wall);
    this.wallData = wall.stream().collect(Collectors.toMap(Function.identity(), k -> -1));
  }

  @Override
  public @NonNull UpdateResult update() {
    int counter = 0;
    while (!wall.isEmpty() && ++counter <= 6) {
      Block random = wall.get(ThreadLocalRandom.current().nextInt(wall.size()));
      if (tryBreakBlock(random)) {
        wall.remove(random);
        wallData.remove(random);
      }
    }
    return wall.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  private boolean tryBreakBlock(Block block) {
    //noinspection ConstantConditions
    int progress = wallData.computeIfPresent(block, (k, v) -> v + 1);
    if (progress <= 9) {
      PacketUtil.fakeBreak(block.getWorld(), Vector3d.center(block), (byte) progress);
      return false;
    }
    destroyBlock(block);
    return true;
  }

  private void destroyBlock(Block block) {
    Vector3d velocity = VectorUtil.gaussianOffset(Vector3d.ZERO, 0.2, 0.1, 0.2);
    TempBlock.air().duration(Bending.properties().earthRevertTime()).build(block);
    TempFallingBlock.builder(Material.MAGMA_BLOCK.createBlockData()).velocity(velocity).duration(10000).build(block);
  }

  public static @Nullable Fracture of(@NonNull List<@NonNull Block> wall) {
    return wall.isEmpty() ? null : new Fracture(wall);
  }
}
