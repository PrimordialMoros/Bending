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

package me.moros.bending.api.platform.entity;

import java.util.Set;
import java.util.function.Predicate;

import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.potion.Potion;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.platform.potion.PotionEffectTag;
import me.moros.bending.api.platform.world.WorldUtil;

/**
 * Utility class with useful {@link Entity} related methods.
 * <p>Note: This is not thread-safe.
 */
public final class EntityUtil {
  private EntityUtil() {
  }

  /**
   * Check if a user is against a wall made of blocks matching the given predicate.
   * <p>Note: Passable blocks and barriers are ignored.
   * @param entity the entity to check
   * @param predicate the type of blocks to accept
   * @return whether the user is against a wall
   */
  public static boolean isAgainstWall(Entity entity, Predicate<Block> predicate) {
    Block origin = entity.block();
    for (Direction face : WorldUtil.SIDES) {
      Block relative = origin.offset(face);
      if (!relative.type().isCollidable() || relative.type() == BlockType.BARRIER) {
        continue;
      }
      if (predicate.test(relative)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Removes any negative potion effects the entity may have.
   * @param entity the entity to process
   */
  public static void removeNegativeEffects(LivingEntity entity) {
    entity.activePotions().stream().map(Potion::effect)
      .filter(PotionEffectTag.HARMFUL::isTagged).forEach(entity::removePotion);
  }

  private static final Set<PotionEffect> TICKING_EFFECT = Set.of(
    PotionEffect.REGENERATION, PotionEffect.POISON, PotionEffect.WITHER, PotionEffect.HUNGER
  );

  /**
   * Attempt to add a potion effect of a specified duration and amplifier.
   * The applied potion effect will only override an existing one if the duration or amplifier is bigger.
   * @param entity the entity to process
   * @param type the type of potion effect to apply
   * @param duration the duration of the potion effect in ticks
   * @param amplifier the potion effect amplifier starting from 0
   * @return if a new potion effect was added, false otherwise
   */
  public static boolean tryAddPotion(Entity entity, PotionEffect type, int duration, int amplifier) {
    if (amplifier >= 0 && duration > 0 && entity.valid() && entity instanceof LivingEntity livingEntity) {
      int minDuration = TICKING_EFFECT.contains(type) ? 1 : (PotionEffectTag.BENEFICIAL.isTagged(type) ? duration : 20);
      Potion potion = livingEntity.potion(type);
      if (potion == null || potion.duration() < minDuration || potion.amplifier() < amplifier) {
        livingEntity.addPotion(type.builder().duration(duration).amplifier(amplifier).build());
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
  public static boolean tryRemovePotion(Entity entity, PotionEffect type, int maxDuration, int maxAmplifier) {
    if (entity.valid() && entity instanceof LivingEntity livingEntity) {
      Potion potion = livingEntity.potion(type);
      if (potion != null && potion.duration() <= maxDuration && potion.amplifier() <= maxAmplifier) {
        livingEntity.removePotion(type);
        return true;
      }
    }
    return false;
  }
}
