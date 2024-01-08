/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.ability.common.basic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.function.Predicate;

import me.moros.bending.api.ability.state.State;
import me.moros.bending.api.ability.state.StateChain;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import me.moros.math.Vector3i;
import me.moros.math.VectorUtil;

public abstract class BlockStream implements State {
  private StateChain chain;
  private final User user;
  private final Collection<Collider> colliders = new ArrayList<>();
  private final BlockType type;

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
  protected BlockStream(User user, BlockType type, double range, int speed) {
    this.user = user;
    this.type = type;
    this.range = range;
    this.speed = Math.min(20, speed);
    buffer = speed;
  }

  @Override
  public void start(StateChain chain) {
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
  public UpdateResult update() {
    buffer += speed;
    if (buffer < 20) {
      return UpdateResult.CONTINUE;
    }
    buffer -= 20;

    if (!started || stream.stream().noneMatch(this::isValid)) {
      return UpdateResult.REMOVE;
    }

    Block head = stream.getFirst();
    Vector3d current = head.center();
    if (controllable || direction == null) {
      Vector3d targetLoc = user.rayTrace(range).cast(user.world()).entityEyeLevelOrPosition();
      // Improve targeting when near
      if (head.distanceSq(targetLoc.floor()) < 1.1) {
        targetLoc = targetLoc.add(user.direction());
      }
      direction = targetLoc.subtract(current).normalize();
    }

    Vector3d originalVector = current;
    Block originBlock = user.world().blockAt(originalVector);

    current = current.add(direction);
    head = user.world().blockAt(current);
    if (!user.canBuild(head)) {
      return UpdateResult.REMOVE;
    }

    clean(stream.removeLast());
    if (current.distanceSq(user.eyeLocation()) <= range * range) {
      boolean canRender = true;
      for (Vector3i v : VectorUtil.decomposeDiagonals(originalVector, direction)) {
        Block b = originBlock.offset(v);
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
      Collider collider = AABB.EXPANDED_BLOCK_BOUNDS.at(block);
      colliders.add(collider);
      hit |= CollisionUtil.handle(user, collider, this::onEntityHit, livingOnly, false);
    }

    return hit ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  public void postRender() {
  }

  public abstract boolean onEntityHit(Entity entity);

  public void onBlockHit(Block block) {
  }

  public Collection<Collider> colliders() {
    return colliders;
  }

  protected void renderHead(Block block) {
    if (type == BlockType.WATER && MaterialUtil.isWater(block)) {
      ParticleBuilder.bubble(block).spawn(user.world());
    } else {
      TempBlock.builder(type).build(block);
    }
  }

  public boolean isValid(Block block) {
    if (type == BlockType.WATER) {
      return MaterialUtil.isWater(block);
    }
    return type == block.type();
  }

  public void cleanAll() {
    stream.forEach(this::clean);
  }

  private void clean(Block block) {
    if (isValid(block)) {
      TempBlock.air().build(block);
    }
  }
}
