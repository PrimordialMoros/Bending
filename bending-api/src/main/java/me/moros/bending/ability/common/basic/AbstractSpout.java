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

package me.moros.bending.ability.common.basic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.model.ability.SimpleAbility;
import me.moros.bending.model.ability.Updatable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.manager.FlightManager.Flight;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractSpout implements Updatable, SimpleAbility {
  private final User user;
  protected final Set<Block> ignore = new HashSet<>();
  protected final Flight flight;

  protected Predicate<Block> validBlock = x -> true;
  protected AABB collider;

  private final double height;

  protected double distance;

  protected AbstractSpout(@NonNull Flight flight, double height) {
    this.flight = flight;
    this.user = flight.user();
    this.height = height;
    this.flight.flying(true);
  }

  @Override
  public @NonNull UpdateResult update() {
    user.entity().setFallDistance(0);
    user.sprinting(false);
    double maxHeight = height + 2; // Buffer for safety
    Block block = blockCast(user.locBlock(), maxHeight, ignore::contains);
    if (block == null || !validBlock.test(block)) {
      return UpdateResult.REMOVE;
    }
    // Remove if player gets too far away from ground.
    distance = user.location().y() - block.getY();
    if (distance > maxHeight) {
      return UpdateResult.REMOVE;
    }
    flight.flying(distance <= height);
    // Create a bounding box for collision that extends through the spout from the ground to the player.
    collider = new AABB(new Vector3d(-0.5, -distance, -0.5), new Vector3d(0.5, 0, 0.5)).at(user.location());
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

  public static void limitVelocity(@NonNull Entity entity, @NonNull Vector3d velocity, double speed) {
    if (velocity.lengthSq() > speed * speed) {
      entity.setVelocity(velocity.normalize().multiply(speed).clampVelocity());
    }
  }

  public static @Nullable Block blockCast(@NonNull Block origin, double distance) {
    return blockCast(origin, distance, b -> false);
  }

  public static @Nullable Block blockCast(@NonNull Block origin, double distance, @NonNull Predicate<Block> ignore) {
    for (int i = 0; i < distance; i++) {
      Block check = origin.getRelative(BlockFace.DOWN, i);
      boolean isLiquid = check.isLiquid() || MaterialUtil.isWaterPlant(check);
      if ((check.isPassable() && !isLiquid) || ignore.test(check)) {
        continue;
      }
      return check;
    }
    return null;
  }
}

