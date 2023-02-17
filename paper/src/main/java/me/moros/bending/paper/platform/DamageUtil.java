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

package me.moros.bending.paper.platform;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.DamageSource;
import me.moros.bending.api.event.BendingDamageEvent;
import me.moros.bending.api.user.User;
import org.bukkit.EntityEffect;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to handle bending damage and death messages.
 */
public final class DamageUtil {
  private DamageUtil() {
  }

  private static final Cache<UUID, DamageSource> CACHE = Caffeine.newBuilder()
    .expireAfterWrite(100, TimeUnit.MILLISECONDS)
    .scheduler(Scheduler.systemScheduler())
    .build();

  /**
   * Attempt to damage the specified entity with certain parameters.
   * This will fire a {@link BendingDamageEvent} and calculate damage ignoring armor.
   * @param target the entity to damage
   * @param source the user damaging the target
   * @param damage the amount of damage to inflict
   * @param desc the ability which causes the damage
   * @return true if entity was successfully damaged, false otherwise
   */
  public static boolean damageEntity(Entity target, User source, double damage, AbilityDescription desc) {
    Objects.requireNonNull(target);
    Objects.requireNonNull(source);
    Objects.requireNonNull(desc);
    if (damage <= 0 || !canDamage(target)) {
      return false;
    }
    LivingEntity targetEntity = (LivingEntity) target;
    var platformEntity = PlatformAdapter.fromBukkitEntity(targetEntity);
    BendingDamageEvent event = source.game().eventBus().postAbilityDamageEvent(source, desc, platformEntity, damage);
    double dmg = event.damage();
    if (event.cancelled() || dmg <= 0) {
      return false;
    }
    targetEntity.playEffect(EntityEffect.HURT);
    // We only use base damage modifier, so we have to manually calculate other modifiers
    dmg = calculateDamageAfterResistance(targetEntity, dmg);
    if (dmg > 0) {
      dmg = calculateDamageAfterAbsorption(targetEntity, dmg);
      if (dmg > 0) {
        cacheDamageSource(targetEntity.getUniqueId(), DamageSource.of(event.user().name(), event.ability()));
        targetEntity.setLastDamage(dmg);
        double previousHealth = targetEntity.getHealth();
        double newHealth = Math.max(0, previousHealth - dmg);
        targetEntity.setHealth(newHealth);
      }
    }
    return true;
  }

  public static void cacheDamageSource(UUID uuid, @Nullable DamageSource source) {
    if (source != null) {
      CACHE.put(uuid, source);
    }
  }

  public static @Nullable DamageSource cachedDamageSource(UUID uuid) {
    return CACHE.getIfPresent(uuid);
  }

  private static double calculateDamageAfterResistance(LivingEntity entity, double damage) {
    PotionEffect resistance = entity.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
    if (resistance != null) {
      int amplifier = resistance.getAmplifier() + 1;
      if (amplifier >= 5) {
        return 0;
      } else if (amplifier > 0) {
        return damage * (1 - (0.2 * amplifier));
      }
    }
    return damage;
  }

  private static double calculateDamageAfterAbsorption(LivingEntity entity, double damage) {
    double absorption = entity.getAbsorptionAmount();
    if (absorption > 0) {
      if (absorption >= damage) {
        entity.setAbsorptionAmount(absorption - damage);
        return 0;
      } else {
        double delta = damage - absorption;
        entity.setAbsorptionAmount(0);
        return delta;
      }
    }
    return damage;
  }

  private static boolean canDamage(Entity entity) {
    if (entity.isInvulnerable() || !entity.isValid()) {
      return false;
    }
    if (entity instanceof Player player) {
      GameMode mode = player.getGameMode();
      return mode == GameMode.SURVIVAL || mode == GameMode.ADVENTURE;
    }
    return entity instanceof LivingEntity;
  }
}
