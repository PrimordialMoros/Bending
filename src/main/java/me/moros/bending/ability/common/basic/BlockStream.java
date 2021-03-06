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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.function.Predicate;

import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class BlockStream implements State {
  private StateChain chain;
  private final User user;
  private final Collection<Collider> colliders = new ArrayList<>();
  private final Material material;

  protected Predicate<Block> diagonalsPredicate = b -> !MaterialUtil.isTransparentOrWater(b);
  protected Deque<Block> stream;
  protected Vector3d direction;

  private boolean started = false;
  private int buffer;
  private final int speed;

  protected boolean livingOnly = false;
  protected boolean controllable = true;
  protected final double range;

  /**
   * The maximum speed is 20 and represents movement of 1 block per tick.
   * We multiply speed steps by 100 to allow enough control over speed while ensuring accuracy.
   */
  public BlockStream(@NonNull User user, @NonNull Material material, double range, int speed) {
    this.user = user;
    this.material = material;
    this.range = range;
    this.speed = Math.min(20, speed);
    buffer = speed;
  }

  @Override
  public void start(@NonNull StateChain chain) {
    if (started) {
      return;
    }
    this.chain = chain;
    stream = new ArrayDeque<>();
    chain.chainStore().stream().filter(this::isValid).forEach(stream::addLast);
    started = !stream.isEmpty();
  }

  @Override
  public void complete() {
    if (!started) {
      return;
    }
    chain.nextState();
  }

  @Override
  public @NonNull UpdateResult update() {
    buffer += speed;
    if (buffer < 20) {
      return UpdateResult.CONTINUE;
    }
    buffer -= 20;

    if (!started || stream.stream().noneMatch(this::isValid)) {
      return UpdateResult.REMOVE;
    }

    Block head = stream.getFirst();
    Vector3d current = Vector3d.center(head);
    if (controllable || direction == null) {
      Vector3d targetLoc = user.compositeRayTrace(range).result(user.world()).entityEyeLevelOrPosition();
      // Improve targeting when near
      if (new Vector3d(head).distanceSq(targetLoc.floor()) < 1.1) {
        targetLoc = targetLoc.add(user.direction());
      }
      direction = targetLoc.subtract(current).normalize();
    }

    Vector3d originalVector = new Vector3d(current.toArray());
    Block originBlock = originalVector.toBlock(user.world());

    current = current.add(direction);
    head = current.toBlock(user.world());
    if (!user.canBuild(head)) {
      return UpdateResult.REMOVE;
    }

    clean(stream.removeLast());
    if (current.distanceSq(user.eyeLocation()) <= range * range) {
      boolean canRender = true;
      for (Vector3i v : VectorMethods.decomposeDiagonals(originalVector, direction)) {
        Block b = originBlock.getRelative(v.getX(), v.getY(), v.getZ());
        if (diagonalsPredicate.test(b)) {
          canRender = false;
          onBlockHit(b);
          break;
        }
      }
      if (canRender) {
        renderHead(head);
        stream.addFirst(head);
      }
    }

    postRender();

    colliders.clear();
    boolean hit = false;
    for (Block block : stream) {
      Collider collider = AABB.EXPANDED_BLOCK_BOUNDS.at(new Vector3d(block));
      colliders.add(collider);
      hit |= CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, livingOnly, false);
    }

    return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  public void postRender() {
  }

  public abstract boolean onEntityHit(@NonNull Entity entity);

  public void onBlockHit(@NonNull Block block) {
  }

  public @NonNull Collection<@NonNull Collider> colliders() {
    return colliders;
  }

  protected void renderHead(@NonNull Block block) {
    if (material == Material.WATER && MaterialUtil.isWater(block)) {
      ParticleUtil.createBubble(block).spawn();
    } else {
      TempBlock.create(block, material.createBlockData());
    }
  }

  public boolean isValid(@NonNull Block block) {
    if (material == Material.WATER) {
      return MaterialUtil.isWater(block);
    }
    return material == block.getType();
  }

  public void cleanAll() {
    stream.forEach(this::clean);
  }

  private void clean(@NonNull Block block) {
    if (isValid(block)) {
      TempBlock.createAir(block);
    }
  }
}
