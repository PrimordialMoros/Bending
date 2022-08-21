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

package me.moros.bending.util.collision;

import java.util.function.Predicate;

import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempEntity;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Utility class to handle detect and handle ability collisions with entities.
 */
public final class CollisionUtil {
  private CollisionUtil() {
  }

  /**
   * Executes the given callback for every entity intersecting with the given collider.
   * By default, it ignores Spectators, Temporal entities and invisible armor stands.
   * Will also ignore any non-living entities the user.
   * @param user the user (needed for self collision and to specify the world in which collisions are checked)
   * @param collider the collider to check
   * @param callback the method to be called for every hit entity
   * @return true if at least one entity was processed, false otherwise
   * @see #handle(User, Collider, CollisionCallback, boolean, boolean, boolean)
   */
  public static boolean handle(User user, Collider collider, CollisionCallback callback) {
    return handle(user, collider, callback, true, false, false);
  }

  /**
   * Executes the given callback for every entity intersecting with the given collider.
   * By default, it ignores Spectators, Temporal entities and invisible armor stands.
   * Will also ignore the user.
   * @param user the user (needed for self collision and to specify the world in which collisions are checked)
   * @param collider the collider to check
   * @param callback the method to be called for every hit entity
   * @param livingOnly whether only LivingEntities should be checked
   * @return true if at least one entity was processed, false otherwise
   * @see #handle(User, Collider, CollisionCallback, boolean, boolean, boolean)
   */
  public static boolean handle(User user, Collider collider, CollisionCallback callback, boolean livingOnly) {
    return handle(user, collider, callback, livingOnly, false, false);
  }

  /**
   * Executes the given callback for every entity intersecting with the given collider.
   * By default, it ignores Spectators, Temporal entities and invisible armor stands.
   * @param user the user (needed for self collision and to specify the world in which collisions are checked)
   * @param collider the collider to check
   * @param callback the method to be called for every hit entity
   * @param livingOnly whether only LivingEntities should be checked
   * @param selfCollision whether the collider can collider with the user
   * @return true if at least one entity was processed, false otherwise
   * @see #handle(User, Collider, CollisionCallback, boolean, boolean, boolean)
   */
  public static boolean handle(User user, Collider collider, CollisionCallback callback, boolean livingOnly, boolean selfCollision) {
    return handle(user, collider, callback, livingOnly, selfCollision, false);
  }

  /**
   * Executes the given callback for every entity intersecting with the given collider.
   * By default, it ignores Spectators, Temporal entities and invisible armor stands.
   * @param user the user (needed for self collision and to specify the world in which collisions are checked)
   * @param collider the collider to check
   * @param callback the method to be called for every hit entity
   * @param livingOnly whether only LivingEntities should be checked
   * @param selfCollision whether the collider can collider with the user
   * @param earlyEscape if true it will return on the first valid collision callback without evaluating other entities
   * @return true if at least one entity was processed, false otherwise
   */
  public static boolean handle(User user, Collider collider, CollisionCallback callback, boolean livingOnly, boolean selfCollision, boolean earlyEscape) {
    Vector3d extent = collider.halfExtents();
    Vector3d pos = collider.position();
    boolean hit = false;
    Predicate<Entity> filter = entityPredicate(user.entity(), livingOnly, selfCollision);
    for (Entity entity : user.world().getNearbyEntities(pos.toLocation(user.world()), extent.x(), extent.y(), extent.z(), filter)) {
      if (collider.intersects(AABBUtil.entityBounds(entity))) {
        if (!user.canBuild(entity.getLocation().getBlock())) {
          continue;
        }
        boolean result = callback.onEntityHit(entity);
        if (earlyEscape && result) {
          return true;
        }
        hit |= result;
      }
    }
    return hit;
  }

  private static Predicate<Entity> entityPredicate(Entity source, boolean livingOnly, boolean selfCollision) {
    Predicate<Entity> livingPredicate = livingOnly ? e -> e instanceof LivingEntity : e -> true;
    Predicate<Entity> selfPredicate = !selfCollision ? e -> !e.equals(source) : e -> true;
    Predicate<Entity> valid = CollisionUtil::isValidEntity;
    return selfPredicate.and(livingPredicate).and(valid);
  }

  private static boolean isValidEntity(Entity entity) {
    if (entity instanceof Player player) {
      return player.getGameMode() != GameMode.SPECTATOR;
    } else if (entity instanceof FallingBlock) {
      return !TempEntity.MANAGER.isTemp(entity.getEntityId());
    } else if (entity instanceof ArmorStand armorStand) {
      return armorStand.isVisible();
    }
    return true;
  }

  /**
   * Represents the callback when handling entity collisions.
   */
  @FunctionalInterface
  public interface CollisionCallback {
    /**
     * Called when a collision with an entity has been detected.
     * @param entity the entity that collided.
     * @return true if the entity was hit, false otherwise
     */
    boolean onEntityHit(Entity entity);
  }
}
