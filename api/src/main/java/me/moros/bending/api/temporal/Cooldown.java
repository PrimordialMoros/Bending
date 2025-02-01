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

package me.moros.bending.api.temporal;

import java.util.Objects;
import java.util.UUID;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.user.User;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class Cooldown extends Temporary {
  public static final TemporalManager<Cooldown, Cooldown> MANAGER = new TemporalManager<>(2400, false);

  private final UUID uuid;
  private final AbilityDescription desc;
  private final Runnable runnable;
  private boolean reverted = false;
  private final int hashCode;

  private Cooldown(UUID uuid, AbilityDescription desc, @Nullable Runnable runnable) {
    this.uuid = uuid;
    this.desc = desc;
    this.runnable = runnable;
    this.hashCode = Objects.hash(this.uuid, this.desc);
  }

  private Cooldown(UUID uuid, AbilityDescription desc, Runnable runnable, int ticks) {
    this(uuid, desc, runnable);
    MANAGER.addEntry(this, this, ticks);
  }

  @Override
  public boolean revert() {
    if (reverted) {
      return false;
    }
    reverted = true;
    MANAGER.removeEntry(this);
    if (runnable != null) {
      runnable.run();
    }
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Cooldown other) {
      return uuid.equals(other.uuid) && desc.equals(other.desc);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public static Cooldown of(User user, AbilityDescription desc) {
    return new Cooldown(user.uuid(), desc, null);
  }

  public static Cooldown of(User user, AbilityDescription desc, Runnable runnable, long duration) {
    return new Cooldown(user.uuid(), desc, runnable, MANAGER.fromMillis(duration));
  }
}
