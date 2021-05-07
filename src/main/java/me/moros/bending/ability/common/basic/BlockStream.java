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
import java.util.Collections;
import java.util.Deque;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.IntVector;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class BlockStream implements State {
  private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(new Vector3(0.4, 0.4, 0.4));

  private StateChain chain;
  private final User user;
  private final Collection<Collider> colliders = new ArrayList<>();
  private final Material material;

  protected Predicate<Block> diagonalsPredicate = b -> !MaterialUtil.isTransparentOrWater(b);
  protected Deque<Block> stream;
  protected Vector3 direction;

  private boolean started = false;
  private int buffer;
  private final int speed;

  protected boolean livingOnly = false;
  protected boolean controllable = true;
  protected final double range;

  /**
   * The maximum speed is 100 and represents movement of 1 block per tick.
   * Example: A speed of 75 means that the stream will advance 15 (75/100 * 20) blocks in a full cycle (20 ticks).
   * We multiply speed steps by 100 to allow enough control over speed while ensuring accuracy.
   */
  public BlockStream(@NonNull User user, @NonNull Material material, double range, int speed) {
    this.user = user;
    this.material = material;
    this.range = range;
    this.speed = Math.min(100, speed);
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
    // Since this moves block by block, our only choice is to skip an update every x ticks based on our buffer speed
    buffer += speed;
    if (buffer < 100) {
      return UpdateResult.CONTINUE;
    }
    if (!started || stream.stream().noneMatch(this::isValid)) {
      return UpdateResult.REMOVE;
    }

    Block head = stream.getFirst();
    Vector3 current = Vector3.center(head);
    if (controllable || direction == null) {
      Vector3 targetLoc = user.rayTraceEntity(range).map(EntityMethods::entityCenter)
        .orElseGet(() -> user.rayTrace(range, Collections.singleton(material)));
      // Improve targeting when near
      if (new Vector3(head).distanceSq(targetLoc.floor()) < 1.1) {
        targetLoc = targetLoc.add(user.direction());
      }
      direction = targetLoc.subtract(current).normalize();
    }

    buffer -= 100; // Reduce buffer by one since we moved

    Vector3 originalVector = new Vector3(current.toArray());
    Block originBlock = originalVector.toBlock(user.world());

    current = current.add(direction);
    head = current.toBlock(user.world());
    if (!Bending.game().protectionSystem().canBuild(user, head)) {
      return UpdateResult.REMOVE;
    }

    clean(stream.removeLast());
    if (current.distanceSq(user.eyeLocation()) <= range * range) {
      boolean canRender = true;
      for (IntVector v : VectorMethods.decomposeDiagonals(originalVector, direction)) {
        Block b = originBlock.getRelative(v.x, v.y, v.z);
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
      Collider collider = BOX.at(new Vector3(block));
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
      ParticleUtil.create(Particle.WATER_BUBBLE, block.getLocation().add(0.5, 0.5, 0.5))
        .count(5).offset(0.25, 0.25, 0.25).spawn();
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
