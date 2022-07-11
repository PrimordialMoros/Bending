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

package me.moros.bending.model.user;

import java.util.Objects;

import me.moros.bending.model.ability.Updatable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.entity.Player;

public final class BendingBar implements Updatable {
  private final BossBar bar;
  private final Audience audience;

  private final long duration;

  private long endTime;

  private BendingBar(BossBar bar, Audience audience, long duration) {
    this.bar = bar;
    this.audience = audience;
    this.duration = duration;
  }

  @Override
  public UpdateResult update() {
    long time = System.currentTimeMillis();
    if (endTime == 0) {
      endTime = time + duration;
    } else {
      if (time > endTime) {
        onRemove();
        return UpdateResult.REMOVE;
      }
    }
    float factor = Math.max(0, Math.min(1, (endTime - time) / (float) duration));
    audience.showBossBar(bar.progress(factor));
    return UpdateResult.CONTINUE;
  }

  public void onRemove() {
    audience.hideBossBar(bar);
  }

  public static BendingBar of(BossBar bar, Player target, long duration) {
    Objects.requireNonNull(bar);
    Objects.requireNonNull(target);
    return new BendingBar(bar, target, duration);
  }
}
