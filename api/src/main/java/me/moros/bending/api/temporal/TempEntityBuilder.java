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

package me.moros.bending.api.temporal;

import java.util.Objects;

import me.moros.bending.api.platform.world.World;
import me.moros.math.Vector3d;

@SuppressWarnings("unchecked")
abstract class TempEntityBuilder<T, R, B extends TempEntityBuilder<T, R, B>> {
  protected final T data;

  protected Vector3d velocity = Vector3d.ZERO;
  protected boolean gravity = true;
  protected long duration = 30_000;

  TempEntityBuilder(T data) {
    this.data = data;
  }

  public B velocity(Vector3d velocity) {
    this.velocity = Objects.requireNonNull(velocity).clampVelocity();
    return (B) this;
  }

  public B gravity(boolean gravity) {
    this.gravity = gravity;
    return (B) this;
  }

  public B duration(long duration) {
    this.duration = duration;
    return (B) this;
  }

  public abstract R build(World world, Vector3d center);
}
