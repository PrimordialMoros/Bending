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

package me.moros.bending.util;

import me.moros.bending.Bending;
import me.moros.bending.events.BendingDamageEvent;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.EntityEffect;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Utility class to handle bending damage and death messages.
 */
public final class DamageUtil {
  public static boolean damageEntity(@NonNull Entity target, @NonNull User source, double damage, @NonNull AbilityDescription desc) {
    if (target instanceof LivingEntity && target.isValid() && damage > 0) {
      LivingEntity targetEntity = (LivingEntity) target;

      BendingDamageEvent event = Bending.eventBus().postAbilityDamageEvent(source, targetEntity, desc, damage);
      double dmg = event.getFinalDamage();
      if (event.isCancelled() || dmg <= 0) {
        return false;
      }

      // We only use base damage modifier so we have to manually calculate other modifiers
      dmg = calculateDamageAfterResistance(targetEntity, dmg);
      if (dmg > 0) {
        dmg = calculateDamageAfterAbsorption(targetEntity, dmg);
        if (dmg > 0) {
          double previousHealth = targetEntity.getHealth();
          double newHealth = FastMath.max(0, previousHealth - dmg);
          targetEntity.setHealth(newHealth);
        }
      }
      targetEntity.playEffect(EntityEffect.HURT);
      targetEntity.setLastDamageCause(event);
      targetEntity.setLastDamage(dmg);
      return true;
    }
    return false;
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
}
