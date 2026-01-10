/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.api.platform.world;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import me.moros.bending.api.collision.raytrace.BlockRayTrace;
import me.moros.bending.api.collision.raytrace.CompositeRayTrace;
import me.moros.bending.api.collision.raytrace.Context;
import me.moros.bending.api.collision.raytrace.RayTrace;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.particle.ParticleContext;
import me.moros.math.Position;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.key.Keyed;

public interface World extends Keyed, ForwardingAudience, BlockGetter, BlockSetter, EntityAccessor {
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
   * Perform a raytrace for entities and return the result/
   * @param context the raytrace context
   * @param range the range override
   * @return the result of the performed raytrace
   */
  CompositeRayTrace rayTraceEntities(Context context, double range);

  /**
   * Perform a raytrace for both blocks and entities and return the result.
   * @param context the raytrace context
   * @return the result of the performed raytrace
   */
  default CompositeRayTrace rayTrace(Context context) {
    var blockResult = rayTraceBlocks(context);
    double blockHitDistance = blockResult.hit() ? blockResult.position().distance(context.origin()) : context.range();
    var entityResult = rayTraceEntities(context, blockHitDistance);
    var block = blockResult.block();
    if (block == null) {
      return entityResult;
    }
    if (entityResult.hit()) {
      double distSq = context.origin().distanceSq(entityResult.position());
      if (distSq < (blockHitDistance * blockHitDistance)) {
        return entityResult;
      }
    }
    return RayTrace.hit(blockResult.position(), block);
  }

  @Override
  default boolean setBlockStateFast(int x, int y, int z, BlockState state) {
    return Platform.instance().nativeAdapter().setBlockFast(new Block(this, x, y, z), state);
  }

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

  Entity createEntity(Position pos, EntityType type);

  boolean addEntity(Entity entity);

  default Optional<Entity> spawnEntity(Position pos, EntityType type, Consumer<Entity> consumer) {
    Entity entity = createEntity(pos, type);
    consumer.accept(entity);
    return addEntity(entity) ? Optional.of(entity) : Optional.empty();
  }

  default Entity dropItem(Position pos, ItemSnapshot item) {
    return dropItem(pos, item, true);
  }

  Entity dropItem(Position pos, ItemSnapshot item, boolean canPickup);

  Entity createFallingBlock(Position pos, BlockState state, boolean gravity);

  default int lightLevel(Position position) {
    return lightLevel(position.blockX(), position.blockY(), position.blockZ());
  }

  int lightLevel(int x, int y, int z);

  default int blockLightLevel(Position position) {
    return blockLightLevel(position.blockX(), position.blockY(), position.blockZ());
  }

  int blockLightLevel(int x, int y, int z);

  default int skyLightLevel(Position position) {
    return skyLightLevel(position.blockX(), position.blockY(), position.blockZ());
  }

  int skyLightLevel(int x, int y, int z);

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
