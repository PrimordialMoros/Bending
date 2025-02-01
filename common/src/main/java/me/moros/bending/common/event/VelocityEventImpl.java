/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.common.event;

import java.util.Objects;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.event.VelocityEvent;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.common.event.base.AbstractCancellableAbilityEvent;
import me.moros.math.Vector3d;

public class VelocityEventImpl extends AbstractCancellableAbilityEvent implements VelocityEvent {
  private final LivingEntity target;
  private Vector3d velocity;

  public VelocityEventImpl(User user, LivingEntity target, AbilityDescription desc, Vector3d velocity) {
    super(user, desc);
    this.target = target;
    this.velocity = velocity;
  }

  @Override
  public LivingEntity target() {
    return target;
  }

  @Override
  public Vector3d velocity() {
    return velocity;
  }

  @Override
  public void velocity(Vector3d velocity) {
    this.velocity = Objects.requireNonNull(velocity);
  }
}
