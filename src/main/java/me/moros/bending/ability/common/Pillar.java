/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.ability.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.atlas.cf.checker.index.qual.NonNegative;
import me.moros.atlas.cf.checker.index.qual.Positive;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;

public class Pillar implements Updatable {
  private final User user;
  private final Block origin;
  private final BlockFace direction;
  private final BlockFace opposite;
  private final Collection<Block> pillarBlocks;
  private final Predicate<Block> predicate;

  private final boolean solidify;
  private final int length;
  private final int distance;
  private final long interval;
  private final long duration;

  private int currentLength;
  private long nextUpdateTime;

  protected Pillar(@NonNull PillarBuilder builder) {
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
    solidify = (direction != BlockFace.UP && direction != BlockFace.DOWN);
    currentLength = 0;
    nextUpdateTime = 0;
  }

  @Override
  public @NonNull UpdateResult update() {
    if (currentLength >= distance) {
      return UpdateResult.REMOVE;
    }

    long time = System.currentTimeMillis();
    int offset = currentLength + (time >= nextUpdateTime ? 2 : 1);
    AABB collider = AABB.BLOCK_BOUNDS.grow(new Vector3(0, 0.65, 0)).at(new Vector3(origin.getRelative(direction, offset)));
    CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, false, true); // Push entities

    if (time < nextUpdateTime) {
      return UpdateResult.CONTINUE;
    }

    nextUpdateTime = time + interval;
    Block currentIndex = origin.getRelative(direction, ++currentLength);
    return move(currentIndex) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private boolean move(Block newBlock) {
    if (MaterialUtil.isLava(newBlock)) {
      return false;
    }
    if (!MaterialUtil.isTransparentOrWater(newBlock)) {
      return false;
    }
    BlockMethods.tryBreakPlant(newBlock);

    SelectedSource.tryRevertSource(newBlock.getRelative(opposite));

    for (int i = 0; i < length; i++) {
      Block forwardBlock = newBlock.getRelative(opposite, i);
      Block backwardBlock = forwardBlock.getRelative(opposite);
      if (!predicate.test(backwardBlock)) {
        TempBlock.createAir(forwardBlock, duration);
        playSound(forwardBlock);
        return false;
      }
      BlockData data = TempBlock.getLastValidData(backwardBlock);
      TempBlock.create(forwardBlock, solidify ? MaterialUtil.getSolidType(data) : data, duration, true);
    }
    pillarBlocks.add(newBlock);
    TempBlock.createAir(newBlock.getRelative(opposite, length), duration);
    playSound(newBlock);
    return true;
  }

  private Vector3 normalizeVelocity(Vector3 velocity) {
    double factor = 0.75 * (length - 0.4 * currentLength) / length;
    switch (direction) {
      case NORTH:
      case SOUTH:
        return velocity.setX(direction.getDirection().getX() * factor);
      case EAST:
      case WEST:
        return velocity.setZ(direction.getDirection().getZ() * factor);
      case UP:
      case DOWN:
      default:
        return velocity.setY(direction.getDirection().getY() * factor);
    }
  }

  public @NonNull Collection<Block> getPillarBlocks() {
    return Collections.unmodifiableCollection(pillarBlocks);
  }

  public @NonNull Block getOrigin() {
    return origin;
  }

  public void playSound(@NonNull Block block) {
    SoundUtil.EARTH_SOUND.play(block.getLocation());
  }

  public boolean onEntityHit(@NonNull Entity entity) {
    entity.setVelocity(normalizeVelocity(new Vector3(entity.getVelocity())).clampVelocity());
    return true;
  }

  public static @NonNull PillarBuilder builder(@NonNull User user, @NonNull Block origin) {
    return builder(user, origin, Pillar::new);
  }

  public static <T extends Pillar> @NonNull PillarBuilder builder(@NonNull User user, @NonNull Block origin, @NonNull Function<PillarBuilder, T> constructor) {
    return new PillarBuilder(user, origin, constructor);
  }

  public static class PillarBuilder {
    private final User user;
    private final Block origin;
    private final Function<PillarBuilder, ? extends Pillar> constructor;
    private BlockFace direction = BlockFace.UP;
    private int length;
    private int distance;
    private long interval = 125;
    private long duration = BendingProperties.EARTHBENDING_REVERT_TIME;
    private Predicate<Block> predicate = b -> true;

    public <T extends Pillar> PillarBuilder(@NonNull User user, @NonNull Block origin, @NonNull Function<PillarBuilder, T> constructor) {
      this.user = user;
      this.origin = origin;
      this.constructor = constructor;
    }

    public @NonNull PillarBuilder setDirection(@NonNull BlockFace direction) {
      if (!BlockMethods.MAIN_FACES.contains(direction)) {
        throw new IllegalStateException("Pillar direction must be one of the 6 main BlockFaces!");
      }
      this.direction = direction;
      return this;
    }

    public @NonNull PillarBuilder setInterval(@NonNegative long interval) {
      this.interval = interval;
      return this;
    }

    public @NonNull PillarBuilder setDuration(@NonNegative long duration) {
      this.duration = duration;
      return this;
    }

    public @NonNull PillarBuilder setPredicate(@NonNull Predicate<Block> predicate) {
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
      this.distance = FastMath.min(maxLength, maxDistance);
      return Optional.of(constructor.apply(this));
    }

    /**
     * Check region protections and return maximum valid length in blocks
     */
    private int validateLength(int max) {
      for (int i = 0; i < max; i++) {
        Block backwardBlock = origin.getRelative(direction.getOppositeFace(), i);
        if (!TempBlock.isBendable(backwardBlock) || !Bending.getGame().getProtectionSystem().canBuild(user, backwardBlock)) {
          return i;
        }
      }
      return max;
    }

    private int validateDistance(int max) {
      for (int i = 0; i < max; i++) {
        Block forwardBlock = origin.getRelative(direction, i + 1);
        if (!Bending.getGame().getProtectionSystem().canBuild(user, forwardBlock)) {
          return i;
        }
      }
      return max;
    }
  }
}
