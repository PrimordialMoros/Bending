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

package me.moros.bending.event;

import java.util.EnumMap;
import java.util.Map;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.user.User;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Called when a bending ability damages an entity.
 */
@SuppressWarnings("deprecation")
public class BendingDamageEvent extends EntityDamageByEntityEvent implements AbilityEvent {
  private final User user;
  private final AbilityDescription desc;

  public BendingDamageEvent(User user, Entity target, AbilityDescription desc, double damage) {
    super(user.entity(), target, DamageCause.CUSTOM, new EnumMap<>(Map.of(DamageModifier.BASE, damage)), Map.of(DamageModifier.BASE, o -> -0.0), false);
    this.user = user;
    this.desc = desc;
  }

  @Override
  public User user() {
    return user;
  }

  @Override
  public AbilityDescription ability() {
    return desc;
  }
}
