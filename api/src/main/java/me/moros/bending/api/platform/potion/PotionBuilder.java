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

package me.moros.bending.api.platform.potion;

public class PotionBuilder {
  private final PotionEffect effect;
  private int duration = 100;
  private int amplifier = 0;
  private boolean ambient = true;
  private boolean particles = false;
  private boolean icon = false;

  PotionBuilder(PotionEffect effect) {
    this.effect = effect;
  }

  public PotionBuilder duration(int duration) {
    this.duration = duration;
    return this;
  }

  public PotionBuilder amplifier(int amplifier) {
    this.amplifier = amplifier;
    return this;
  }

  public PotionBuilder ambient(boolean ambient) {
    this.ambient = ambient;
    return this;
  }

  public PotionBuilder particles(boolean particles) {
    this.particles = particles;
    return this;
  }

  public PotionBuilder icon(boolean icon) {
    this.icon = icon;
    return this;
  }

  public Potion build() {
    return new PotionImpl(effect, duration, amplifier, ambient, particles, icon);
  }
}
