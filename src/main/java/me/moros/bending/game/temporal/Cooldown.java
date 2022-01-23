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

package me.moros.bending.game.temporal;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.temporal.TemporalManager;
import me.moros.bending.model.temporal.Temporary;
import me.moros.bending.model.temporal.TemporaryBase;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class Cooldown extends TemporaryBase {
  public static final TemporalManager<Cooldown, Cooldown> MANAGER = new TemporalManager<>("Cooldown", Function.identity()::apply);

  public static void init() {
  }

  private final UUID uuid;
  private final AbilityDescription desc;
  private final Runnable runnable;
  private boolean reverted = false;
  private final int hashCode;

  private Cooldown(User user, AbilityDescription desc, Runnable runnable) {
    super();
    this.uuid = user.uuid();
    this.desc = desc;
    this.runnable = runnable;
    this.hashCode = Objects.hash(this.uuid, this.desc);
  }

  private Cooldown(User user, AbilityDescription desc, Runnable runnable, long duration) {
    this(user, desc, runnable);
    MANAGER.addEntry(this, this, Temporary.toTicks(duration));
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
    return obj instanceof Cooldown other && uuid.equals(other.uuid) && desc.equals(other.desc);
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public static @NonNull Cooldown of(@NonNull User user, @NonNull AbilityDescription desc) {
    return new Cooldown(user, desc, null);
  }

  public static @NonNull Cooldown of(@NonNull User user, @NonNull AbilityDescription desc, @NonNull Runnable runnable, long duration) {
    return new Cooldown(user, desc, runnable, duration);
  }
}
