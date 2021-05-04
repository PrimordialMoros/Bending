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

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.Flight;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class AbstractSpout implements Updatable, SimpleAbility {
  private final User user;
  protected final Set<Block> ignore = new HashSet<>();
  protected final Flight flight;

  protected Predicate<Block> validBlock = x -> true;
  protected AABB collider;

  private final double height;

  protected double distance;

  public AbstractSpout(@NonNull User user, double height) {
    this.user = user;
    this.height = height;
    this.flight = Flight.get(user);
    this.flight.flying(true);
  }

  @Override
  public @NonNull UpdateResult update() {
    double maxHeight = height + 2; // Buffer for safety
    Block block = WorldMethods.blockCast(user.world(), new Ray(user.location(), Vector3.MINUS_J), maxHeight, ignore).orElse(null);
    if (block == null || !validBlock.test(block)) {
      return UpdateResult.REMOVE;
    }
    // Remove if player gets too far away from ground.
    distance = user.location().getY() - block.getY();
    if (distance > maxHeight) {
      return UpdateResult.REMOVE;
    }
    flight.flying(distance <= height);
    // Create a bounding box for collision that extends through the spout from the ground to the player.
    collider = new AABB(new Vector3(-0.5, -distance, -0.5), new Vector3(0.5, 0, 0.5)).at(user.location());
    render();
    postRender();
    return UpdateResult.CONTINUE;
  }

  @Override
  public boolean onEntityHit(@NonNull Entity entity) {
    return true;
  }

  @Override
  public boolean onBlockHit(@NonNull Block block) {
    return true;
  }

  @Override
  public @NonNull Collider collider() {
    return collider;
  }

  public @NonNull Flight flight() {
    return flight;
  }

  public static void limitVelocity(@NonNull User user, @NonNull Vector3 velocity, double speed) {
    if (velocity.getNormSq() > speed * speed) {
      user.entity().setVelocity(velocity.normalize().toVector().multiply(speed));
    }
  }
}

