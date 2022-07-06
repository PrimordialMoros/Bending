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

package me.moros.bending.ability.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.properties.BendingProperties;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.util.BoundingBox;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Pillar implements Updatable, Iterable<Block> {
  private final User user;
  private final Block origin;
  private final BlockFace direction;
  private final BlockFace opposite;
  private final Collection<Block> pillarBlocks;
  private final Predicate<Block> predicate;

  private final int length;
  private final int distance;
  private final long interval;
  private final long duration;

  private int currentDistance;
  private long nextUpdateTime;

  protected Pillar(@NonNull Builder builder) {
    this.user = builder.user;
    this.origin = builder.origin;
    this.direction = builder.direction;
    this.opposite = direction.getOppositeFace();
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
  public @NonNull UpdateResult update() {
    if (currentDistance >= distance) {
      return UpdateResult.REMOVE;
    }

    Block start = origin.getRelative(direction, currentDistance + 1);
    Block finish = start.getRelative(opposite, length - 1);
    BoundingBox box = BoundingBox.of(start, finish).expand(direction, 0.65);
    AABB collider = new AABB(new Vector3d(box.getMin()), new Vector3d(box.getMax()));
    CollisionUtil.handle(user, collider, this::onEntityHit, false, true); // Push entities

    long time = System.currentTimeMillis();
    if (time < nextUpdateTime) {
      return UpdateResult.CONTINUE;
    }

    nextUpdateTime = time + interval;
    Block currentIndex = origin.getRelative(direction, ++currentDistance);
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

    SelectedSource.tryRevertSource(newBlock.getRelative(opposite));

    for (int i = 0; i < length; i++) {
      Block forwardBlock = newBlock.getRelative(opposite, i);
      Block backwardBlock = forwardBlock.getRelative(opposite);
      if (!predicate.test(backwardBlock)) {
        TempBlock.air().duration(duration).build(forwardBlock);
        playSound(forwardBlock);
        return false;
      }
      BlockData data = TempBlock.getLastValidData(backwardBlock);
      TempBlock.builder(MaterialUtil.solidType(data)).bendable(true).duration(duration).build(forwardBlock);
    }
    pillarBlocks.add(newBlock);
    TempBlock.air().duration(duration).build(newBlock.getRelative(opposite, length));
    playSound(newBlock);
    return true;
  }

  protected @NonNull Vector3d normalizeVelocity(Vector3d velocity, double factor) {
    return switch (direction) {
      case NORTH, SOUTH -> velocity.withX(direction.getModX() * factor);
      case EAST, WEST -> velocity.withZ(direction.getModZ() * factor);
      default -> velocity.withY(direction.getModY() * factor);
    };
  }

  @Override
  public @NonNull Iterator<Block> iterator() {
    return Collections.unmodifiableCollection(pillarBlocks).iterator();
  }

  public @NonNull Collection<@NonNull Block> pillarBlocks() {
    return List.copyOf(pillarBlocks);
  }

  public @NonNull Block origin() {
    return origin;
  }

  public void playSound(@NonNull Block block) {
    SoundUtil.EARTH.play(block);
  }

  public boolean onEntityHit(@NonNull Entity entity) {
    double factor = 0.75 * (length - 0.4 * currentDistance) / length;
    entity.setVelocity(normalizeVelocity(new Vector3d(entity.getVelocity()), factor).clampVelocity());
    return true;
  }

  public static @NonNull Builder builder(@NonNull User user, @NonNull Block origin) {
    return builder(user, origin, Pillar::new);
  }

  public static <T extends Pillar> @NonNull Builder builder(@NonNull User user, @NonNull Block origin, @NonNull Function<Builder, T> constructor) {
    return new Builder(user, origin, constructor);
  }

  public static class Builder {
    private final User user;
    private final Block origin;
    private final Function<Builder, ? extends Pillar> constructor;
    private BlockFace direction = BlockFace.UP;
    private int length;
    private int distance;
    private long interval = 125;
    private long duration = BendingProperties.instance().earthRevertTime();
    private Predicate<Block> predicate = b -> true;

    public <T extends Pillar> Builder(@NonNull User user, @NonNull Block origin, @NonNull Function<Builder, T> constructor) {
      this.user = user;
      this.origin = origin;
      this.constructor = constructor;
    }

    public @NonNull Builder direction(@NonNull BlockFace direction) {
      if (!WorldUtil.FACES.contains(direction)) {
        throw new IllegalStateException("Pillar direction must be one of the 6 main BlockFaces!");
      }
      this.direction = direction;
      return this;
    }

    public @NonNull Builder interval(@NonNegative long interval) {
      this.interval = interval;
      return this;
    }

    public @NonNull Builder duration(@NonNegative long duration) {
      this.duration = duration;
      return this;
    }

    public @NonNull Builder predicate(@NonNull Predicate<Block> predicate) {
      this.predicate = predicate;
      return this;
    }

    public Optional<Pillar> build(@Positive int length) {
      return build(length, length);
    }

    public Optional<Pillar> build(@Positive int length, @Positive int distance) {
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
        Block backwardBlock = origin.getRelative(direction.getOppositeFace(), i);
        if (!TempBlock.isBendable(backwardBlock) || !user.canBuild(backwardBlock) || !predicate.test(backwardBlock)) {
          return i;
        }
      }
      return max;
    }

    private int validateDistance(int max) {
      for (int i = 0; i < max; i++) {
        Block forwardBlock = origin.getRelative(direction, i + 1);
        if (!user.canBuild(forwardBlock)) {
          return i;
        }
      }
      return max;
    }
  }
}
