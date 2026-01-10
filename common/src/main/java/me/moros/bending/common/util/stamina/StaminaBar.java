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

package me.moros.bending.common.util.stamina;

import java.util.Objects;

import me.moros.bending.api.ability.Updatable;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;

public final class StaminaBar implements Updatable {
  private static final int INTERVAL_FACTOR = 20; // 20 ticks per seconds
  private static final int REGEN_DELAY = 30; // 1.5 second

  public enum State {IDLE, DRAINING, PRE_REGENERATING, REGENERATING}

  private final BossBar bar;
  private final Audience audience;
  private final StaminaGauge gauge;

  private final int staminaRegen;

  private State state = State.IDLE;
  private int ticksUntilRegen = REGEN_DELAY;
  private boolean visible = false;

  StaminaBar(Builder builder, Audience audience) {
    this.bar = builder.bar;
    this.audience = audience;
    this.gauge = new StaminaGauge(INTERVAL_FACTOR * builder.maxStamina, INTERVAL_FACTOR * builder.stamina);
    this.staminaRegen = builder.staminaRegen;
  }

  @Override
  public UpdateResult update() {
    if (gauge.isEmpty()) {
      reset();
      return UpdateResult.REMOVE;
    }
    handleState();
    return UpdateResult.CONTINUE;
  }

  private void handleState() {
    if (state == State.PRE_REGENERATING) {
      if (--ticksUntilRegen <= 0) {
        resetTicksForRegen();
        state = State.REGENERATING;
      }
    } else if (state == State.REGENERATING) {
      if (!gauge.increment(staminaRegen)) {
        hide();
      }
    }

    if (state != State.IDLE) {
      bar.progress(gauge.progress());
    }

    if (!gauge.isFull()) {
      show();
    }
  }

  private void show() {
    if (!visible) {
      audience.showBossBar(bar);
      visible = true;
    }
  }

  private void hide() {
    if (visible) {
      audience.hideBossBar(bar);
      visible = false;
    }
  }

  public boolean hasAtLeast(int amount) {
    return gauge.hasAtLeast(INTERVAL_FACTOR * amount);
  }

  public boolean fill(int amount) {
    return gauge.increment(INTERVAL_FACTOR * amount);
  }

  public boolean drainCost(int amount) {
    state = State.DRAINING;
    return gauge.decrement(INTERVAL_FACTOR * amount);
  }

  public boolean tickingDrain(int amount) {
    state = State.DRAINING;
    return gauge.decrement(amount);
  }

  public void reset() {
    hide();
    if (state == State.DRAINING) {
      state = State.PRE_REGENERATING;
      resetTicksForRegen();
    }
  }

  private void resetTicksForRegen() {
    ticksUntilRegen = REGEN_DELAY;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private BossBar bar;
    private int maxStamina = 200;
    private int stamina = 0; // default to max stamina
    private int staminaRegen = 25;

    public Builder bar(BossBar bar) {
      this.bar = bar;
      return this;
    }

    public Builder maxStamina(int maxStamina) {
      this.maxStamina = Math.max(1, maxStamina);
      return this;
    }

    public Builder stamina(int stamina) {
      this.stamina = stamina;
      return this;
    }

    public Builder staminaRegen(int staminaRegen) {
      this.staminaRegen = staminaRegen;
      return this;
    }

    private void validate() {
      Objects.requireNonNull(bar);
      if (stamina <= 0 || stamina > maxStamina) {
        stamina = maxStamina;
      }
    }

    public StaminaBar build(Audience audience) {
      Objects.requireNonNull(audience);
      validate();
      return new StaminaBar(this, audience);
    }
  }
}
