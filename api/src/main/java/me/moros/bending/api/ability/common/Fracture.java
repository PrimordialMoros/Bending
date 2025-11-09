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

package me.moros.bending.api.ability.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.util.Tasker;
import me.moros.math.FastMath;
import org.jspecify.annotations.Nullable;

public class Fracture implements Updatable {
  private final Collection<Block> wall;
  private final Collection<Block> weakened;
  private final Map<Block, Integer> wallData;
  private final FragileStructure.Builder<?> fragileBuilder;
  private final long interval;
  private long nextUpdateTime;

  protected <T extends Fracture> Fracture(Builder<T> builder) {
    this.wall = builder.blocks.stream().sorted(Comparator.comparingInt(Block::blockY)).collect(Collectors.toList());
    this.weakened = new HashSet<>();
    this.wallData = this.wall.stream().collect(Collectors.toMap(Function.identity(), k -> -1));
    this.fragileBuilder = builder.fragileBuilder;
    this.interval = builder.interval;
  }

  @Override
  public UpdateResult update() {
    if (interval > 50) {
      long currentTime = System.currentTimeMillis();
      if (currentTime < nextUpdateTime) {
        return UpdateResult.CONTINUE;
      }
      nextUpdateTime = currentTime + interval;
    }
    Iterator<Block> iterator = wall.iterator();
    if (iterator.hasNext()) {
      Block block = iterator.next();
      int offset = 1;
      int maxY = block.blockY();
      while (block != null) {
        int y = ThreadLocalRandom.current().nextBoolean() ? maxY : maxY + offset;
        if (block.blockY() <= y && tryBreakBlock(block)) {
          iterator.remove();
          TempBlock.builder(BlockType.MAGMA_BLOCK).build(block);
          weakened.add(block);
          wallData.remove(block);
        } else if (block.blockY() > maxY + offset) {
          break;
        }
        block = iterator.hasNext() ? iterator.next() : null;
      }
    }
    if (wall.isEmpty()) {
      if (fragileBuilder != null) {
        FragileStructure structure = fragileBuilder.add(weakened).build();
        if (structure != null) {
          Tasker.sync().submit(() -> FragileStructure.tryDamageStructure(weakened, 0, Ray.ZERO), 200);
        }
      }
      return UpdateResult.REMOVE;
    }
    return UpdateResult.CONTINUE;
  }

  private boolean tryBreakBlock(Block block) {
    //noinspection ConstantConditions
    int progress = wallData.computeIfPresent(block, (k, v) -> v + 1);
    int particles = FastMath.floor(0.5 * progress);
    Particle.LAVA.builder(block.center()).count(particles).offset(0.4).spawn(block.world());
    if (progress <= 9) {
      Platform.instance().nativeAdapter().fakeBreak(block, (byte) progress).broadcast(block.world(), block);
      return false;
    }
    return true;
  }

  public static Builder<Fracture> builder() {
    return builder(Fracture::new);
  }

  public static <T extends Fracture> Builder<T> builder(Function<Builder<T>, T> constructor) {
    return new Builder<>(constructor);
  }

  public static final class Builder<T extends Fracture> {
    private final Function<Builder<T>, T> constructor;
    private final Collection<Block> blocks = new ArrayList<>();
    private FragileStructure.Builder<?> fragileBuilder;
    private long interval = 0;

    private Builder(Function<Builder<T>, T> constructor) {
      this.constructor = constructor;
    }

    public Builder<T> interval(long interval) {
      this.interval = Math.max(0, interval);
      return this;
    }

    public Builder<T> add(Collection<Block> blocks) {
      this.blocks.addAll(List.copyOf(blocks));
      return this;
    }

    public Builder<T> fragile(FragileStructure.@Nullable Builder<?> fragileBuilder) {
      this.fragileBuilder = fragileBuilder;
      return this;
    }

    public @Nullable Fracture build() {
      if (blocks.isEmpty()) {
        return null;
      }
      return constructor.apply(this);
    }
  }
}
