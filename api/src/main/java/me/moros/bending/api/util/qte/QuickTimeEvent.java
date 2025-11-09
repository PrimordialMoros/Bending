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

package me.moros.bending.api.util.qte;

import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.Updatable;
import me.moros.math.FastMath;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

public abstract class QuickTimeEvent implements Updatable {
  private static final int VALID_TICK_RANGE = 7; // 350ms

  private enum State {PENDING_ATTEMPT, SUCCESS, FAILURE, HANDLED}

  protected final Audience audience;
  protected final Component title;
  protected final BossBar bar;
  protected final int duration;
  protected final int targetTick;

  private State state;
  private int ticks;

  protected QuickTimeEvent(Audience audience, Component title, int duration) {
    this.audience = audience;
    this.title = title;
    this.bar = BossBar.bossBar(this.title, 0, Color.YELLOW, Overlay.NOTCHED_10);
    this.targetTick = FastMath.round(duration * 0.1 * ThreadLocalRandom.current().nextInt(1, 9));
    this.duration = duration;
    this.state = State.PENDING_ATTEMPT;
    this.ticks = 0;
  }

  @Override
  public UpdateResult update() {
    if (state != State.PENDING_ATTEMPT || ticks > duration) {
      onRemove();
      return UpdateResult.REMOVE;
    }
    float factor = Math.clamp((duration - ticks) / (float) duration, 0, 1);
    bar.name(formatTitle()).progress(factor);
    if (ticks++ == 0) {
      audience.showBossBar(bar);
    }
    if (ticks == targetTick) {
      notifyTick();
    }
    return UpdateResult.CONTINUE;
  }

  private Component formatTitle() {
    return title.decoration(TextDecoration.UNDERLINED, isFocused());
  }

  public boolean isFocused() {
    return ticks >= targetTick && ticks <= targetTick + VALID_TICK_RANGE;
  }

  public void attempt() {
    if (state != State.PENDING_ATTEMPT) {
      return;
    }
    state = isFocused() ? State.SUCCESS : State.FAILURE;
  }

  protected abstract void onSuccess();

  protected abstract void onFailure();

  protected void notifyTick() {
  }

  public void onRemove() {
    if (state == State.HANDLED) {
      return;
    }
    audience.hideBossBar(bar);
    switch (state) {
      case PENDING_ATTEMPT, FAILURE -> onFailure();
      case SUCCESS -> onSuccess();
    }
    state = State.HANDLED;
  }
}
