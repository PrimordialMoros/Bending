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

package me.moros.bending.api.event;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.event.base.AbstractCancellableAbilityEvent;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.user.User;

/**
 * Called when a bending ability damages an entity.
 */
public class BendingDamageEvent extends AbstractCancellableAbilityEvent {
  private final LivingEntity target;
  private double damage;

  protected BendingDamageEvent(User user, AbilityDescription desc, LivingEntity target, double damage) {
    super(user, desc);
    this.target = target;
    this.damage = damage;
  }

  public LivingEntity target() {
    return target;
  }

  public double damage() {
    return damage;
  }

  public void damage(double damage) {
    this.damage = damage;
  }
}
