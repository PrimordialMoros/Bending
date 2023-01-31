/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.platform.entity;

import me.moros.bending.event.VelocityEvent;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.data.DataHolder;
import me.moros.bending.platform.Direction;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.world.World;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

public interface Entity extends Identity, ForwardingAudience.Single, Damageable, DataHolder {
  int id();

  Component name();

  World world();

  EntityType type();

  default Key worldKey() {
    return world().key();
  }

  double width();

  double height();

  int yaw();

  int pitch();

  /**
   * Calculates a vector at the center of the given entity using its height.
   * @return the resulting vector
   */
  default Vector3d center() {
    return location().add(0, height() / 2, 0);
  }

  default Block block() {
    return world().blockAt(location());
  }

  Vector3d location();

  Vector3d direction();

  Vector3d velocity();

  void velocity(Vector3d velocity);

  /**
   * Set this entity's velocity and post a {@link VelocityEvent} if it's a LivingEntity.
   * @param ability the ability the causes this velocity change
   * @param velocity the new velocity
   * @return whether the new velocity was successfully applied
   */
  default boolean applyVelocity(Ability ability, Vector3d velocity) {
    velocity(velocity);
    return true;
  }

  boolean valid();

  boolean dead();

  int maxFreezeTicks();

  int freezeTicks();

  void freezeTicks(int ticks);

  int maxFireTicks();

  int fireTicks();

  void fireTicks(int ticks);

  default AABB bounds() {
    return dimensions(location());
  }

  default AABB dimensions(Position pos) {
    double hw = 0.5 * width();
    Vector3d min = Vector3d.of(pos.x() - hw, pos.y(), pos.z() - hw);
    Vector3d max = Vector3d.of(pos.x() + hw, pos.y() + height(), pos.z() + hw);
    return new AABB(min, max);
  }

  /**
   * Check if this entity is standing on ground.
   * @return true if entity standing on ground, false otherwise
   */
  boolean isOnGround();

  /**
   * Calculates the distance between this entity and the ground using precise {@link AABB} colliders.
   * By default, it ignores all passable materials except liquids.
   * @return the distance in blocks between this entity and ground or the max world height.
   * @see #distanceAboveGround(double)
   */
  default double distanceAboveGround() {
    return distanceAboveGround(world().height());
  }

  /**
   * Calculates the distance between this entity and the ground using precise {@link AABB} colliders.
   * By default, it ignores all passable materials except liquids.
   * @param maxHeight the maximum height to check
   * @return the distance in blocks between the entity and ground or the max height.
   */
  default double distanceAboveGround(double maxHeight) {
    int minHeight = world().minHeight();
    double bottomY = location().y();
    AABB entityBounds = bounds().grow(Vector3d.of(0, maxHeight, 0));
    Block origin = block();
    for (int i = 0; i < maxHeight; i++) {
      Block check = origin.offset(Direction.DOWN, i);
      if (check.blockY() <= minHeight) {
        break;
      }
      AABB checkBounds = check.type().isLiquid() ? AABB.BLOCK_BOUNDS.at(check) : check.bounds();
      if (checkBounds.intersects(entityBounds)) {
        return Math.max(0, bottomY - checkBounds.max.y());
      }
    }
    return maxHeight;
  }

  /**
   * Check if entity is submerged underwater.
   * @param fullySubmerged whether to check if entity is partially or fully submerged
   * @return the result
   */
  boolean inWater(boolean fullySubmerged);

  /**
   * Check if entity is submerged under lava.
   * @param fullySubmerged whether to check if entity is partially or fully submerged
   * @return the result
   */
  boolean inLava(boolean fullySubmerged);

  boolean visible();

  double fallDistance();

  void fallDistance(double distance);

  void remove();

  boolean isProjectile();

  boolean gravity();

  void gravity(boolean value);

  boolean invulnerable();

  void invulnerable(boolean value);

  boolean teleport(Position position);
}
