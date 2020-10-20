/*
 *   Copyright 2020 Moros <https://github.com/PrimordialMoros>
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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.user.User;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.time.Duration;
import java.util.UUID;

/**
 * Utility class to handle bending damage and player death messages.
 */
public final class DamageUtil {
	private static final Cache<UUID, String> cache = Caffeine.newBuilder()
		.maximumSize(100)
		.expireAfterWrite(Duration.ofSeconds(60))
		.build();

	public static boolean damageEntity(@NonNull Entity entity, @NonNull User source, double damage, @Nullable AbilityDescription desc) {
		if (entity instanceof LivingEntity && damage > 0) {
			LivingEntity targetEntity = (LivingEntity) entity;
			LivingEntity sourceEntity = source.getEntity();
			EntityDamageByEntityEvent finalEvent = new EntityDamageByEntityEvent(sourceEntity, entity, EntityDamageEvent.DamageCause.CUSTOM, damage);
			targetEntity.damage(damage, sourceEntity);
			targetEntity.setLastDamageCause(finalEvent);
			if (desc != null && targetEntity instanceof Player) cache.put(targetEntity.getUniqueId(), desc.getName());
			return true;
		}
		return false;
	}


	public static boolean damageEntity(@NonNull Entity entity, @NonNull User source, double damage) {
		return damageEntity(entity, source, damage, null);
	}

	public static String getBendingMessage(@NonNull UUID uuid) {
		return cache.getIfPresent(uuid);
	}

	private static class BendingDeath {
		// TODO death messages
	}
}
