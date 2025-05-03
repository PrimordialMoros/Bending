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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class Pillar implements Updatable, Iterable<Block> {
  private final User user;
  private final Block origin;
  private final Direction direction;
  private final Collection<Block> pillarBlocks;
  private final Predicate<Block> predicate;

  private final int length;
  private final int distance;
  private final long interval;
  private final long duration;

  private int currentDistance;
  private long nextUpdateTime;

  protected <T extends Pillar> Pillar(Builder<T> builder) {
    this.user = builder.user;
    this.origin = builder.origin;
    this.direction = builder.direction;
    this.length = builder.length;
    this.distance = builder.distance;
    this.interval = builder.interval;
    this.duration = builder.duration;
    this.predicate = builder.predicate;
    this.pillarBlocks = new ArrayList<>(length);
    currentDistance = 0;
    nextUpdateTime = 0;
  }

  @Override
  public UpdateResult update() {
    if (currentDistance >= distance) {
      return UpdateResult.REMOVE;
    }
    Vector3d pos = origin.offset(direction.opposite(), length - currentDistance).center();
    AABB collider = createPillarBox(pos, length + 0.35);
    CollisionUtil.handle(user, collider, this::onEntityHit, false, true); // Push entities

    long time = System.currentTimeMillis();
    if (time < nextUpdateTime) {
      return UpdateResult.CONTINUE;
    }

    nextUpdateTime = time + interval;
    Block currentIndex = origin.offset(direction, ++currentDistance);
    return move(currentIndex) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private boolean move(Block newBlock) {
    if (MaterialUtil.isLava(newBlock)) {
      return false;
    }
    if (!MaterialUtil.isTransparentOrWater(newBlock)) {
      return false;
    }
    WorldUtil.tryBreakPlant(newBlock);

    SelectedSource.tryRevertSource(newBlock.offset(direction.opposite()));

    for (int i = 0; i < length; i++) {
      Block forwardBlock = newBlock.offset(direction, -i);
      Block backwardBlock = forwardBlock.offset(direction.opposite());
      if (!predicate.test(backwardBlock)) {
        TempBlock.air().duration(duration).build(forwardBlock);
        playSound(forwardBlock);
        return false;
      }
      BlockType type = MaterialUtil.solidType(TempBlock.getLastValidType(backwardBlock));
      TempBlock.builder(type).bendable(true).duration(duration).build(forwardBlock);
    }
    pillarBlocks.add(newBlock);
    TempBlock.air().duration(duration).build(newBlock.offset(direction, -length));
    playSound(newBlock);
    return true;
  }

  private Vector3d withComponentInDirection(Vector3d vector, Direction dir, double factor) {
    return switch (dir) {
      case EAST, WEST -> vector.withX(direction.blockX() * factor);
      case NORTH, SOUTH -> vector.withZ(direction.blockZ() * factor);
      default -> vector.withY(direction.blockY() * factor);
    };
  }

  public AABB createPillarBox(Vector3d pos, double len) {
    Vector3d xMin = Vector3d.of(-0.5, -0.5, -0.5);
    Vector3d xMax = Vector3d.of(len + 0.5, 0.5, 0.5);
    Vector3d yMin = Vector3d.of(-0.5, -0.5, -0.5);
    Vector3d yMax = Vector3d.of(0.5, len + 0.5, 0.5);
    Vector3d zMin = Vector3d.of(-0.5, -0.5, -0.5);
    Vector3d zMax = Vector3d.of(0.5, 0.5, len + 0.5);
    return switch (direction) {
      case EAST -> AABB.of(pos.add(xMin), pos.add(xMax));
      case WEST -> AABB.of(pos.add(xMax.negate()), pos.add(xMin.negate()));
      case UP -> AABB.of(pos.add(yMin), pos.add(yMax));
      case DOWN -> AABB.of(pos.add(yMax.negate()), pos.add(yMin.negate()));
      case NORTH -> AABB.of(pos.add(zMin), pos.add(zMax));
      case SOUTH -> AABB.of(pos.add(zMax.negate()), pos.add(zMin.negate()));
    };
  }

  @Override
  public Iterator<Block> iterator() {
    return Collections.unmodifiableCollection(pillarBlocks).iterator();
  }

  public Collection<Block> pillarBlocks() {
    return List.copyOf(pillarBlocks);
  }

  public Block origin() {
    return origin;
  }

  public void playSound(Block block) {
    SoundEffect.EARTH.play(block);
  }

  public boolean onEntityHit(Entity entity) {
    double factor = 0.75 * (length - 0.4 * currentDistance) / length;
    Vector3d vel = withComponentInDirection(entity.velocity(), direction, factor);
    entity.velocity(vel);
    return true;
  }

  public static Builder<Pillar> builder(User user, Block origin) {
    return builder(user, origin, Pillar::new);
  }

  public static <T extends Pillar> Builder<T> builder(User user, Block origin, Function<Builder<T>, T> constructor) {
    return new Builder<>(user, origin, constructor);
  }

  public static final class Builder<T extends Pillar> {
    private final User user;
    private final Block origin;
    private final Function<Builder<T>, T> constructor;
    private Direction direction = Direction.UP;
    private int length;
    private int distance;
    private long interval = 125;
    private long duration = BendingProperties.instance().earthRevertTime();
    private Predicate<Block> predicate = b -> true;

    private Builder(User user, Block origin, Function<Builder<T>, T> constructor) {
      this.user = user;
      this.origin = origin;
      this.constructor = constructor;
    }

    public Builder<T> direction(Direction direction) {
      if (!WorldUtil.FACES.contains(direction)) {
        throw new IllegalStateException("Pillar direction must be one of the 6 main BlockFaces.");
      }
      this.direction = direction;
      return this;
    }

    public Builder<T> interval(@NonNegative long interval) {
      this.interval = interval;
      return this;
    }

    public Builder<T> duration(@NonNegative long duration) {
      this.duration = duration;
      return this;
    }

    public Builder<T> predicate(Predicate<Block> predicate) {
      this.predicate = predicate;
      return this;
    }

    public Optional<T> build(@Positive int length) {
      return build(length, length);
    }

    public Optional<T> build(@Positive int length, @Positive int distance) {
      int maxLength = validateLength(length);
      if (maxLength < 1) {
        return Optional.empty();
      }
      int maxDistance = validateDistance(distance);
      if (maxDistance < 1) {
        return Optional.empty();
      }
      this.length = maxLength;
      this.distance = Math.min(maxLength, maxDistance);
      return Optional.of(constructor.apply(this));
    }

    /**
     * Check region protections and return maximum valid length in blocks
     */
    private int validateLength(int max) {
      for (int i = 0; i < max; i++) {
        Block backwardBlock = origin.offset(direction.opposite(), i);
        if (!TempBlock.isBendable(backwardBlock) || !user.canBuild(backwardBlock) || !predicate.test(backwardBlock)) {
          return i;
        }
      }
      return max;
    }

    private int validateDistance(int max) {
      for (int i = 0; i < max; i++) {
        Block forwardBlock = origin.offset(direction, i + 1);
        if (!user.canBuild(forwardBlock)) {
          return i;
        }
      }
      return max;
    }
  }
}
