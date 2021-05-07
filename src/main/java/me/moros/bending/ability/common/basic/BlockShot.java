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

import java.util.Collections;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class BlockShot implements Updatable, SimpleAbility {
  private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(new Vector3(0.3, 0.3, 0.3));

  private User user;

  private Block current;
  private Block previousBlock;
  private AABB collider;
  private Vector3 firstDestination;

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
    this.current = block;
    this.range = range;
    this.speed = Math.min(100, speed);
    buffer = speed;

    redirect();
    settingUp = true;
    firstDestination = center();
    int targetY = NumberConversions.floor(target.y);
    int currentY = current.getY();
    Vector3 dir = target.subtract(firstDestination).normalize().setY(0);
    if ((targetY < currentY && !current.getRelative(BlockFace.DOWN).isPassable()) || (targetY > currentY && !current.getRelative(BlockFace.UP).isPassable())) {
      firstDestination = firstDestination.add(dir).snapToBlockCenter();
    } else {
      firstDestination = firstDestination.setY(targetY + 0.5);
    }
  }

  @Override
  public @NonNull UpdateResult update() {
    buffer += speed;
    if (buffer < 100) {
      return UpdateResult.CONTINUE;
    }
    buffer -= 100; // Reduce buffer by one since we moved

    clean();
    if (current.getY() == NumberConversions.floor(firstDestination.y)) {
      settingUp = false;
    }
    Vector3 dest = settingUp ? firstDestination : target;
    Vector3 currentVector = center();
    direction = dest.subtract(currentVector).normalize();
    Vector3 originalVector = new Vector3(currentVector.toArray());
    currentVector = currentVector.add(direction).snapToBlockCenter();

    Block originBlock = originalVector.toBlock(user.world());
    for (IntVector v : VectorMethods.decomposeDiagonals(originalVector, direction)) {
      if (diagonalsPredicate.test(originBlock.getRelative(v.x, v.y, v.z))) {
        return UpdateResult.REMOVE;
      }
    }

    if (currentVector.distanceSq(user.eyeLocation()) > range * range) {
      return UpdateResult.REMOVE;
    }

    previousBlock = current;
    current = currentVector.toBlock(user.world());

    if (!Bending.game().protectionSystem().canBuild(user, current)) {
      return UpdateResult.REMOVE;
    }
    collider = BOX.at(center().floor());
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
      } else {
        TempBlock.create(current, material.createBlockData(), false);
      }
    } else {
      return UpdateResult.REMOVE;
    }
    return currentVector.distanceSq(target) < 0.8 ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  public void redirect() {
    target = user.rayTraceEntity(range)
      .map(EntityMethods::entityCenter)
      .orElseGet(() -> user.rayTrace(range, Collections.singleton(material)))
      .snapToBlockCenter();
    settingUp = false;
  }

  public @Nullable Block previousBlock() {
    return previousBlock;
  }

  public @NonNull Vector3 center() {
    return Vector3.center(current);
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
    clean(current);
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
