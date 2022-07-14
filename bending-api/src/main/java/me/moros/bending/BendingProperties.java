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

package me.moros.bending;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public interface BendingProperties {
  final class Holder {
    private static final BendingProperties DEFAULTS = new BendingProperties() {
    };
    private static BendingProperties INSTANCE;

    private Holder() {
    }
  }

  private ThreadLocalRandom rand() {
    return ThreadLocalRandom.current();
  }

  static BendingProperties instance() {
    return Holder.INSTANCE == null ? Holder.DEFAULTS : Holder.INSTANCE;
  }

  static void inject(BendingProperties properties) {
    if (Holder.INSTANCE != null) {
      throw new IllegalStateException("Properties have already been initialized!");
    }
    if (properties == Holder.DEFAULTS) {
      throw new IllegalArgumentException("Injected BendingProperties are invalid!");
    }
    Holder.INSTANCE = Objects.requireNonNull(properties);
  }

  default long earthRevertTime() {
    return 300_000;
  }

  default long earthRevertTime(long delay) {
    return earthRevertTime() + rand().nextLong(delay);
  }

  default long fireRevertTime() {
    return 10_000;
  }

  default long fireRevertTime(long delay) {
    return fireRevertTime() + rand().nextLong(delay);
  }

  default long explosionRevertTime() {
    return 20_000;
  }

  default long explosionRevertTime(long delay) {
    return explosionRevertTime() + rand().nextLong(delay);
  }

  default long iceRevertTime() {
    return 10_000;
  }

  default long iceRevertTime(long delay) {
    return iceRevertTime() + rand().nextLong(delay);
  }

  default double explosionKnockback() {
    return 0.8;
  }

  default double explosionKnockback(double value) {
    return explosionKnockback() * value;
  }

  default double metalModifier() {
    return 1.25;
  }

  default double metalModifier(double value) {
    return metalModifier() * value;
  }

  default double magmaModifier() {
    return 1.4;
  }

  default double magmaModifier(double value) {
    return magmaModifier() * value;
  }

  default double moonModifier() {
    return 1.25;
  }

  default double moonModifier(double value) {
    return moonModifier() * value;
  }

  default double sunModifier() {
    return 1.25;
  }

  default double sunModifier(double value) {
    return sunModifier() * value;
  }

  default boolean canGenerateLight() {
    return true;
  }
}
