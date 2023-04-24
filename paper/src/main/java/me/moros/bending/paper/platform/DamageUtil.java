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
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.user.User;
import me.moros.bending.common.locale.TranslationManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility class to handle bending damage and death messages.
 */
public final class DamageUtil {
  private static TranslationManager manager;

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
    if (damage <= 0) {
      return false;
    }
    LivingEntity targetEntity = (LivingEntity) target;
    var platformEntity = PlatformAdapter.fromBukkitEntity(targetEntity);
    BendingDamageEvent event = source.game().eventBus().postAbilityDamageEvent(source, desc, platformEntity, damage);
    double dmg = event.damage();
    if (!event.cancelled() && dmg > 0 && Platform.instance().nativeAdapter().damage(event, manager::translate)) {
      cacheDamageSource(targetEntity.getUniqueId(), DamageSource.of(event.user().name(), event.ability()));
      return true;
    }
    return false;
  }

  public static void cacheDamageSource(UUID uuid, @Nullable DamageSource source) {
    if (source != null) {
      CACHE.put(uuid, source);
    }
  }

  public static @Nullable DamageSource cachedDamageSource(UUID uuid) {
    return CACHE.getIfPresent(uuid);
  }
}
