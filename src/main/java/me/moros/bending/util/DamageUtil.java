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

import java.time.Duration;
import java.util.UUID;

import me.moros.atlas.caffeine.cache.Cache;
import me.moros.atlas.caffeine.cache.Caffeine;
import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.cf.checker.nullness.qual.Nullable;
import me.moros.bending.Bending;
import me.moros.bending.events.BendingDamageEvent;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Utility class to handle bending damage and death messages.
 */
public final class DamageUtil {
  private static final TranslatableComponent DEATH_MESSAGE = Component.translatable("bending.ability.generic.death");

  private static final Cache<UUID, BendingDamage> cache = Caffeine.newBuilder().expireAfterWrite(Duration.ofMillis(250)).build();

  public static boolean damageEntity(@NonNull Entity target, @NonNull User source, double damage, @NonNull AbilityDescription desc) {
    if (target instanceof LivingEntity && damage > 0) {
      LivingEntity targetEntity = (LivingEntity) target;
      LivingEntity sourceEntity = source.entity();
      BendingDamageEvent event = Bending.eventBus().postAbilityDamageEvent(source, target, desc, damage);
      if (event.isCancelled()) {
        return false;
      }
      if (target instanceof Player) {
        cache.put(target.getUniqueId(), new BendingDamage(source.entity(), desc));
      }
      double finalDamage = event.damage();
      targetEntity.setLastDamageCause(new EntityDamageByEntityEvent(target, sourceEntity, EntityDamageEvent.DamageCause.CUSTOM, finalDamage));
      targetEntity.damage(finalDamage, sourceEntity);
      return true;
    }
    return false;
  }

  public static @Nullable Component bendingDeathMessage(@NonNull Player player) {
    BendingDamage cause = cache.getIfPresent(player.getUniqueId());
    if (cause == null) {
      return null;
    }
    cache.invalidate(player.getUniqueId());
    AbilityDescription ability = cause.desc;
    String deathKey = "bending.ability." + ability.name().toLowerCase() + ".death";
    TranslatableComponent msg = Bending.translationManager().getTranslation(deathKey);
    if (msg == null) {
      msg = DEATH_MESSAGE;
    }
    Component target = Component.text(player.getName());
    Component source = Component.text(cause.source.getName());
    return msg.args(target, source, ability.displayName());
  }

  private static class BendingDamage {
    private final Entity source;
    private final AbilityDescription desc;

    private BendingDamage(@NonNull Entity source, @NonNull AbilityDescription desc) {
      this.source = source;
      this.desc = desc;
    }
  }
}
