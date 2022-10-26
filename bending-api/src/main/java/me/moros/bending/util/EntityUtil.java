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

package me.moros.bending.util;

import java.util.function.Predicate;

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.event.EventBus;
import me.moros.bending.event.VelocityEvent;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.util.collision.AABBUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectType.Category;

/**
 * Utility class with useful {@link Entity} related methods.
 * <p>Note: This is not thread-safe.
 */
public final class EntityUtil {
  private EntityUtil() {
  }

  /**
   * Set an entity's velocity and post a {@link VelocityEvent} if it's a LivingEntity.
   * @param ability the ability the causes this velocity change
   * @param entity the target entity
   * @param velocity the new velocity
   * @return whether the new velocity was successfully applied
   */
  public static boolean applyVelocity(Ability ability, Entity entity, Vector3d velocity) {
    if (entity instanceof LivingEntity livingEntity) {
      VelocityEvent event = EventBus.INSTANCE.postVelocityEvent(ability.user(), livingEntity, ability.description(), velocity);
      if (!event.isCancelled()) {
        entity.setVelocity(event.velocity().clampVelocity());
        return true;
      }
      return false;
    }
    entity.setVelocity(velocity.clampVelocity());
    return true;
  }

  /**
   * Check if a user is against a wall made of blocks matching the given predicate.
   * <p>Note: Passable blocks and barriers are ignored.
   * @param entity the entity to check
   * @param predicate the type of blocks to accept
   * @return whether the user is against a wall
   */
  public static boolean isAgainstWall(Entity entity, Predicate<Block> predicate) {
    Block origin = entity.getLocation().getBlock();
    for (BlockFace face : WorldUtil.SIDES) {
      Block relative = origin.getRelative(face);
      if (relative.isPassable() || relative.getType() == Material.BARRIER) {
        continue;
      }
      if (predicate.test(relative)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Accurately checks if an entity is standing on ground using {@link AABB}.
   * @param entity the entity to check
   * @return true if entity standing on ground, false otherwise
   */
  public static boolean isOnGround(Entity entity) {
    if (!(entity instanceof Player)) {
      return entity.isOnGround();
    }
    AABB entityBounds = AABBUtil.entityBounds(entity).grow(new Vector3d(0, 0.05, 0));
    AABB floorBounds = new AABB(new Vector3d(-1, -0.1, -1), new Vector3d(1, 0.1, 1)).at(new Vector3d(entity.getLocation()));
    for (Block block : WorldUtil.nearbyBlocks(entity.getWorld(), floorBounds, b -> !b.isPassable())) {
      if (entityBounds.intersects(AABBUtil.blockBounds(block))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Calculates the distance between an entity and the ground using precise {@link AABB} colliders.
   * By default, it ignores all passable materials except liquids.
   * @param entity the entity to check
   * @return the distance in blocks between the entity and ground or the max world height.
   * @see #distanceAboveGround(Entity, double)
   */
  public static double distanceAboveGround(Entity entity) {
    int minHeight = entity.getWorld().getMinHeight();
    int realMax = entity.getWorld().getMaxHeight() - minHeight;
    return distanceAboveGround(entity, realMax);
  }

  /**
   * Calculates the distance between an entity and the ground using precise {@link AABB} colliders.
   * By default, it ignores all passable materials except liquids.
   * @param entity the entity to check
   * @param maxHeight the maximum height to check
   * @return the distance in blocks between the entity and ground or the max height.
   */
  public static double distanceAboveGround(Entity entity, double maxHeight) {
    int minHeight = entity.getWorld().getMinHeight();
    AABB entityBounds = AABBUtil.entityBounds(entity).grow(new Vector3d(0, maxHeight, 0));
    Block origin = entity.getLocation().getBlock();
    for (int i = 0; i < maxHeight; i++) {
      Block check = origin.getRelative(BlockFace.DOWN, i);
      if (check.getY() <= minHeight) {
        break;
      }
      AABB checkBounds = check.isLiquid() ? AABB.BLOCK_BOUNDS.at(new Vector3d(check)) : AABBUtil.blockBounds(check);
      if (checkBounds.intersects(entityBounds)) {
        return Math.max(0, entity.getBoundingBox().getMinY() - checkBounds.max.y());
      }
    }
    return maxHeight;
  }

  /**
   * Calculates a vector at the center of the given entity using its height.
   * @param entity the entity to get the vector for
   * @return the resulting vector
   */
  public static Vector3d entityCenter(Entity entity) {
    return new Vector3d(entity.getLocation()).add(0, entity.getHeight() / 2, 0);
  }

  /**
   * Check if entity is submerged underwater.
   * @param entity the entity to check
   * @return the result
   */
  public static boolean underWater(Entity entity) {
    return entity.isInWaterOrBubbleColumn() && NativeAdapter.instance().eyeInWater(entity);
  }

  /**
   * Check if entity is submerged under lava.
   * @param entity the entity to check
   * @return the result
   */
  public static boolean underLava(Entity entity) {
    return entity.isInLava() && NativeAdapter.instance().eyeInLava(entity);
  }

  /**
   * Removes any negative potion effects the entity may have.
   * @param entity the entity to process
   */
  public static void removeNegativeEffects(LivingEntity entity) {
    entity.getActivePotionEffects().stream().map(PotionEffect::getType)
      .filter(t -> t.getEffectCategory() == Category.HARMFUL).forEach(entity::removePotionEffect);
  }

  /**
   * Attempt to add a potion effect of a specified duration and amplifier.
   * The applied potion effect will only override an existing one if the duration or amplifier is bigger.
   * @param entity the entity to process
   * @param type the type of potion effect to apply
   * @param duration the duration of the potion effect in ticks
   * @param amplifier the potion effect amplifier starting from 0
   * @return if a new potion effect was added, false otherwise
   */
  public static boolean tryAddPotion(Entity entity, PotionEffectType type, int duration, int amplifier) {
    if (amplifier > 0 && duration > 0 && entity.isValid() && entity instanceof LivingEntity livingEntity) {
      int minDuration = type.getEffectCategory() == Category.BENEFICIAL ? 20 : duration;
      PotionEffect effect = livingEntity.getPotionEffect(type);
      if (effect == null || effect.getDuration() < minDuration || effect.getAmplifier() < amplifier) {
        livingEntity.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false));
        return true;
      }
    }
    return false;
  }

  /**
   * Attempt to remove a potion effect.
   * The specified potion effect will only be removed if the duration and amplifier are less or equal to the parameters.
   * @param entity the entity to process
   * @param type the type of potion effect to remove
   * @param maxDuration the maximum duration of the potion effect in ticks
   * @param maxAmplifier the maximum potion effect amplifier starting from 0
   * @return if the potion effect was removed, false otherwise
   */
  public static boolean tryRemovePotion(Entity entity, PotionEffectType type, int maxDuration, int maxAmplifier) {
    if (entity.isValid() && entity instanceof LivingEntity livingEntity) {
      PotionEffect effect = livingEntity.getPotionEffect(type);
      if (effect != null && effect.getDuration() <= maxDuration && effect.getAmplifier() <= maxAmplifier) {
        livingEntity.removePotionEffect(type);
        return true;
      }
    }
    return false;
  }
}
