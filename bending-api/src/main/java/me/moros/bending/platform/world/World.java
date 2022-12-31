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

package me.moros.bending.platform.world;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import me.moros.bending.model.raytrace.BlockRayTrace;
import me.moros.bending.model.raytrace.CompositeRayTrace;
import me.moros.bending.model.raytrace.Context;
import me.moros.bending.platform.Direction;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.item.ItemSnapshot;
import me.moros.bending.platform.particle.ParticleContext;
import me.moros.math.Position;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;

public interface World extends Identity, ForwardingAudience, BlockGetter, BlockSetter, EntityGetter {
  @Override
  default Block blockAt(int x, int y, int z) {
    return new Block(this, x, y, z);
  }

  String name();

  int minHeight();

  int maxHeight();

  default int height() {
    return maxHeight() - minHeight();
  }

  <T> void spawnParticle(ParticleContext<T> context);

  /**
   * Perform a raytrace for blocks and return the result.
   * @param context the raytrace context
   * @return the result of the performed raytrace
   */
  BlockRayTrace rayTraceBlocks(Context context);

  /**
   * Perform a raytrace for both blocks and entities and return the result.
   * @param context the raytrace context
   * @return the result of the performed raytrace
   */
  CompositeRayTrace rayTrace(Context context);

  @Override
  default Optional<Block> findTop(Position origin, int height, Predicate<Block> predicate) {
    Block block = new Block(this, origin);
    for (int i = 1; i <= height; i++) {
      Block check = block.offset(Direction.UP, i);
      if (!predicate.test(check)) {
        return Optional.of(check.offset(Direction.DOWN));
      }
    }
    return Optional.empty();
  }

  @Override
  default Optional<Block> findBottom(Position origin, int height, Predicate<Block> predicate) {
    Block block = new Block(this, origin);
    for (int i = 1; i <= height; i++) {
      Block check = block.offset(Direction.DOWN, i);
      if (!predicate.test(check)) {
        return Optional.of(check.offset(Direction.UP));
      }
    }
    return Optional.empty();
  }

  /**
   * Check if it is daytime in this world. Only applicable in the Overworld.
   * @return true if it is daytime
   * @see #isNight()
   */
  boolean isDay();

  /**
   * Check if it is nighttime in this world. Only applicable in the Overworld.
   * @return true if it is nighttime
   * @see #isDay()
   */
  boolean isNight();

  boolean breakNaturally(Position position);

  default Entity dropItem(Position position, ItemSnapshot item) {
    return dropItem(position, item, true);
  }

  Entity dropItem(Position position, ItemSnapshot item, boolean canPickup);

  int lightLevel(Position position);

  Dimension dimension();

  default CompletableFuture<?> loadChunkAsync(Position position) {
    return loadChunkAsync(position.blockX() >> 4, position.blockZ() >> 4);
  }

  CompletableFuture<?> loadChunkAsync(int x, int z);

  int viewDistance();

  enum Dimension {
    OVERWORLD,
    NETHER,
    END,
    CUSTOM
  }
}
