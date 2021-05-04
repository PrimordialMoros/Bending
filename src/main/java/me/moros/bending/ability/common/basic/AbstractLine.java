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

import me.moros.bending.Bending;
import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractLine extends MovementResolver implements Updatable, SimpleAbility {
  private final User user;

  protected final Vector3 origin;

  protected Vector3 location;
  protected Vector3 targetLocation;
  protected Vector3 direction;
  protected Collider collider;
  protected LivingEntity target;

  protected final double range;
  protected final double speed;

  protected boolean locked = false;
  protected boolean controllable = false;
  protected boolean skipVertical = false;

  public AbstractLine(@NonNull User user, @NonNull Block source, double range, double speed, boolean followTarget) {
    super(user.world());
    this.user = user;
    this.location = new Vector3(source.getLocation().add(0.5, 1.25, 0.5));
    this.origin = location;
    this.range = range;
    this.speed = speed;
    target = user.rayTraceEntity(range).orElse(null);
    if (followTarget && target != null) {
      targetLocation = EntityMethods.entityCenter(target);
      locked = true;
    } else {
      target = null;
      targetLocation = user.rayTrace(range, Collections.singleton(Material.WATER));
    }
    direction = targetLocation.subtract(location).setY(0).normalize();
  }

  @Override
  public @NonNull UpdateResult update() {
    if (locked) {
      if (isValidTarget()) {
        targetLocation = new Vector3(target.getLocation());
        direction = targetLocation.subtract(location).setY(0).normalize();
      } else {
        locked = false;
      }
    }

    if (controllable) {
      targetLocation = user.rayTrace(range);
      direction = targetLocation.subtract(origin).setY(0).normalize();
    }

    if (onBlockHit(location.toBlock(user.world()).getRelative(BlockFace.DOWN))) {
      return UpdateResult.REMOVE;
    }

    collider = new Sphere(location, 1);
    if (CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, true)) {
      return UpdateResult.REMOVE;
    }

    Vector3 newLocation = resolve(location, direction);
    if (newLocation == null) {
      onCollision();
      return UpdateResult.REMOVE;
    }

    render();
    location = location.add(newLocation).scalarMultiply(0.5);
    render(); // Render again at midpoint for a smoother line
    postRender();
    location = newLocation;

    Block block = location.toBlock(user.world());

    if (skipVertical) { // Advance location vertically if possible to match target height
      int y1 = NumberConversions.floor(targetLocation.getY());
      int y2 = NumberConversions.floor(newLocation.getY());
      if (y1 > y2 && isValidBlock(block.getRelative(BlockFace.UP))) {
        location = newLocation.add(Vector3.PLUS_J);
        block = block.getRelative(BlockFace.UP);
      } else if (y1 < y2 && isValidBlock(block.getRelative(BlockFace.DOWN))) {
        location = newLocation.add(Vector3.MINUS_J);
        block = block.getRelative(BlockFace.DOWN);
      }
    }

    if (location.distanceSq(origin) > range * range) {
      return UpdateResult.REMOVE;
    }
    if (!Bending.game().protectionSystem().canBuild(user, block)) {
      return UpdateResult.REMOVE;
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public @NonNull Collider collider() {
    return collider;
  }

  protected void onCollision() {
  }

  protected boolean isValidTarget() {
    if (target == null || !target.isValid()) {
      return false;
    }
    if (target instanceof Player && !((Player) target).isOnline()) {
      return false;
    }
    return target.getWorld().equals(user.world()) && targetLocation.distanceSq(new Vector3(target.getLocation())) < 5 * 5;
  }
}
