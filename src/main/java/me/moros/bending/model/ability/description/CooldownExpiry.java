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

package me.moros.bending.model.ability.description;

import java.util.concurrent.TimeUnit;

import me.moros.atlas.caffeine.cache.Expiry;
import org.apache.commons.math3.util.FastMath;

public class CooldownExpiry implements Expiry<AbilityDescription, Long> {
  @Override
  public long expireAfterCreate(AbilityDescription key, Long cooldown, long currentTime) {
    return TimeUnit.MILLISECONDS.toNanos(cooldown);
  }

  @Override
  public long expireAfterUpdate(AbilityDescription key, Long cooldown, long currentTime, long currentDuration) {
    return FastMath.max(currentDuration, TimeUnit.MILLISECONDS.toNanos(cooldown));
  }

  @Override
  public long expireAfterRead(AbilityDescription key, Long cooldown, long currentTime, long currentDuration) {
    return currentDuration;
  }
}

