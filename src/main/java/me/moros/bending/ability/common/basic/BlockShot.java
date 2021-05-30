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

package me.moros.bending.ability.common.basic;

import java.util.function.Predicate;

import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BlockShot implements Updatable, SimpleAbility {
  private User user;

  private Vector3 location;
  private Block previousBlock;
  private Block tempBlock;
  private AABB collider;
  private final Vector3 firstDestination;

  protected Predicate<Block> diagonalsPredicate = b -> !MaterialUtil.isTransparentOrWater(b);
  protected Vector3 target;
  protected Vector3 direction;
  protected Material material;

  private boolean settingUp;
  private int buffer;
  private final int speed;

  protected boolean allowUnderWater = false;
  protected final double range;

  /**
   * The maximum speed is 100 and represents movement of 1 block per tick.
   * Example: A speed of 75 means that the stream will advance 15 (75/100 * 20) blocks in a full cycle (20 ticks).
   * We multiply speed steps by 100 to allow enough control over speed while ensuring accuracy.
   */
  public BlockShot(@NonNull User user, @NonNull Block block, double range, int speed) {
    this.user = user;
    this.material = block.getType();
    this.location = Vector3.center(block);
    this.range = range;
    this.speed = Math.min(100, speed);
    buffer = speed;

    redirect();
    settingUp = true;
    int targetY = NumberConversions.floor(target.y);
    int currentY = block.getY();
    Vector3 dir = target.subtract(location).normalize().setY(0);
    boolean validAbove = block.getRelative(BlockFace.UP).isPassable();
    boolean validBelow = block.getRelative(BlockFace.DOWN).isPassable();
    Vector3 fixedY = location.setY(targetY + 0.5);
    if ((targetY > currentY && validAbove) || (targetY < currentY && validBelow)) {
      firstDestination = fixedY;
    } else if (location.add(dir).toBlock(user.world()).isPassable()) {
      firstDestination = location.add(dir).snapToBlockCenter();
    } else {
      if (validAbove) {
        firstDestination = location.add(new Vector3(0, 2, 0));
      } else if (validBelow) {
        firstDestination = location.add(new Vector3(0, -2, 0));
      } else {
        firstDestination = fixedY;
      }
    }
    direction = firstDestination.subtract(location).normalize();
  }

  @Override
  public @NonNull UpdateResult update() {
    buffer += speed;
    if (buffer < 100) {
      return UpdateResult.CONTINUE;
    }
    buffer -= 100; // Reduce buffer by one since we moved

    clean();
    if (Math.abs(location.y - firstDestination.y) < 0.5) {
      settingUp = false;
    }
    previousBlock = location.toBlock(user.world());
    Vector3 dest = settingUp ? firstDestination : target;
    direction = dest.subtract(location.subtract(direction)).normalize();
    Vector3 originalVector = new Vector3(location.toArray());
    Block originBlock = originalVector.toBlock(user.world());
    if (location.add(direction).toBlock(user.world()).equals(previousBlock)) {
      direction = direction.multiply(2);
    }
    for (IntVector v : VectorMethods.decomposeDiagonals(originalVector, direction)) {
      if (diagonalsPredicate.test(originBlock.getRelative(v.x, v.y, v.z))) {
        return UpdateResult.REMOVE;
      }
    }
    if (originalVector.add(direction).distanceSq(user.eyeLocation()) > range * range) {
      return UpdateResult.REMOVE;
    }
    location = location.add(direction);
    Block current = location.toBlock(user.world());
    if (!user.canBuild(current)) {
      return UpdateResult.REMOVE;
    }
    collider = AABB.EXPANDED_BLOCK_BOUNDS.at(location.floor());
    if (CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit)) {
      return UpdateResult.REMOVE;
    }
    if (!MaterialUtil.isTransparent(current)) {
      onBlockHit(current);
    }
    if (MaterialUtil.isTransparent(current) || (MaterialUtil.isWater(current) && allowUnderWater)) {
      BlockMethods.tryBreakPlant(current);
      if (material == Material.WATER && MaterialUtil.isWater(current)) {
        ParticleUtil.create(Particle.WATER_BUBBLE, current.getLocation().add(0.5, 0.5, 0.5))
          .count(5).offset(0.25, 0.25, 0.25).spawn();
        tempBlock = null;
      } else {
        tempBlock = current;
        TempBlock.create(current, material.createBlockData(), false);
      }
    } else {
      return UpdateResult.REMOVE;
    }
    return location.distanceSq(target) < 0.8 ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  public void redirect() {
    Block ignore = location.toBlock(user.world());
    target = user.rayTraceEntity(range)
      .map(e -> new Vector3(e.getEyeLocation()))
      .orElseGet(() -> user.rayTrace(range, b -> b.equals(ignore)))
      .snapToBlockCenter();
    settingUp = false;
  }

  public @Nullable Block previousBlock() {
    return previousBlock;
  }

  public @NonNull Vector3 center() {
    return location.floor().add(new Vector3(0.5, 0.5, 0.5));
  }

  @Override
  public void render() {
  }

  @Override
  public @NonNull AABB collider() {
    return collider;
  }

  public boolean isValid(@NonNull Block block) {
    if (material == Material.WATER) {
      return MaterialUtil.isWater(block);
    }
    return material == block.getType();
  }

  public void clean() {
    if (tempBlock != null) {
      clean(tempBlock);
    }
  }

  public void clean(@NonNull Block block) {
    if (isValid(block)) {
      TempBlock.createAir(block);
    }
  }

  public void user(@NonNull User user) {
    this.user = user;
  }
}
