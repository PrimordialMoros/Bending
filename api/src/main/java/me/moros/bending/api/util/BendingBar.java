/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.api.util;

import java.util.Objects;

import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.platform.entity.player.Player;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;

/**
 * Represents a {@link BossBar} for bending purposes.
 */
public final class BendingBar implements Updatable {
  private final BossBar bar;
  private final Audience audience;

  private final int duration;
  private int ticks;

  private BendingBar(BossBar bar, Audience audience, int duration) {
    this.bar = bar;
    this.audience = audience;
    this.duration = duration;
  }

  @Override
  public UpdateResult update() {
    if (ticks >= duration) {
      onRemove();
      return UpdateResult.REMOVE;
    }
    float factor = Math.clamp((duration - ticks) / (float) duration, 0, 1);
    bar.progress(factor);
    if (ticks++ == 0) {
      audience.showBossBar(bar);
    }
    return UpdateResult.CONTINUE;
  }

  public void onRemove() {
    audience.hideBossBar(bar);
  }

  public static BendingBar of(BossBar bar, Player target, int ticks) {
    Objects.requireNonNull(bar);
    Objects.requireNonNull(target);
    return new BendingBar(bar, target, ticks);
  }
}
