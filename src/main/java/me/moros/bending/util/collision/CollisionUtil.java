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

import me.moros.bending.game.temporal.TempFallingBlock;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import org.bukkit.GameMode;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class CollisionUtil {
  private CollisionUtil() {
  }

  /**
   * @return {@link #handle(User, Collider, CollisionCallback, boolean, boolean)} with living entities only and selfCollision, earlyEscape disabled
   */
  public static boolean handle(@NonNull User user, @NonNull Collider collider, @NonNull CollisionCallback callback) {
    return handle(user, collider, callback, true, false, false);
  }

  /**
   * @return {@link #handle(User, Collider, CollisionCallback, boolean, boolean)} with selfCollision and earlyEscape disabled
   */
  public static boolean handle(@NonNull User user, @NonNull Collider collider, @NonNull CollisionCallback callback, boolean livingOnly) {
    return handle(user, collider, callback, livingOnly, false, false);
  }

  /**
   * @return {@link #handle(User, Collider, CollisionCallback, boolean, boolean, boolean)} with earlyEscape disabled
   */
  public static boolean handle(@NonNull User user, @NonNull Collider collider, @NonNull CollisionCallback callback, boolean livingOnly, boolean selfCollision) {
    return handle(user, collider, callback, livingOnly, selfCollision, false);
  }

  /**
   * Checks a collider to see if it's hitting any entities near it.
   * By default it ignores Spectators and invisible armor stands.
   * @param user the user (needed for self collision and to specify the world in which collisions are checked)
   * @param collider the collider to check
   * @param callback the method to be called for every hit entity
   * @param livingOnly whether only LivingEntities should be checked
   * @param selfCollision whether the collider can collider with the user
   * @param earlyEscape if true it will return on the first valid collision callback without evaluating other entities
   * @return true if it hit at least one entity
   */
  public static boolean handle(@NonNull User user, @NonNull Collider collider, @NonNull CollisionCallback callback, boolean livingOnly, boolean selfCollision, boolean earlyEscape) {
    Vector3d extent = collider.halfExtents();
    Vector3d pos = collider.position();
    boolean hit = false;
    Predicate<Entity> filter = entityPredicate(user.entity(), livingOnly, selfCollision);
    for (Entity entity : user.world().getNearbyEntities(pos.toLocation(user.world()), extent.getX(), extent.getY(), extent.getZ(), filter)) {
      if (collider.intersects(AABBUtil.entityBounds(entity))) {
        if (!user.canBuild(entity.getLocation().getBlock())) {
          continue;
        }
        boolean result = callback.onCollision(entity);
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
    } else if (entity instanceof FallingBlock fallingBlock) {
      return !TempFallingBlock.MANAGER.isTemp(fallingBlock);
    } else if (entity instanceof ArmorStand armorStand) {
      return armorStand.isVisible();
    }
    return true;
  }

  @FunctionalInterface
  public interface CollisionCallback {
    boolean onCollision(@NonNull Entity entity);
  }
}
