/*
 * Copyright 2020-2026 Moros
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

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.preset.Preset;
import me.moros.bending.api.event.BindChangeEvent;
import me.moros.bending.api.user.User;
import me.moros.bending.common.event.base.AbstractCancellableUserEvent;
import org.jspecify.annotations.Nullable;

public class BindChangeEventImpl {
  public static class Single extends AbstractCancellableUserEvent implements BindChangeEvent.Single {
    private final int slot;
    private final AbilityDescription desc;

    public Single(User user, int slot, @Nullable AbilityDescription desc) {
      super(user);
      this.slot = slot;
      this.desc = desc;
    }

    @Override
    public int slot() {
      return slot;
    }

    @Override
    public @Nullable AbilityDescription ability() {
      return desc;
    }
  }

  public static class Multi extends AbstractCancellableUserEvent implements BindChangeEvent.Multi {
    private final Preset preset;

    public Multi(User user, Preset preset) {
      super(user);
      this.preset = preset;
    }

    @Override
    public Preset preset() {
      return preset;
    }
  }
}
