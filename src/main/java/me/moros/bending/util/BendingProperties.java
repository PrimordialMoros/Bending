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

package me.moros.bending.util;

import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.Configurable;
import org.spongepowered.configurate.CommentedConfigurationNode;

public final class BendingProperties extends Configurable {
  private final ThreadLocalRandom rand;

  private long earthRevertTime;
  private long fireRevertTime;
  private long explosionRevertTime;
  private long iceRevertTime;

  private double explosionKnockback;

  private double metalModifier;
  private double magmaModifier;
  private double moonModifier;
  private double sunModifier;

  public BendingProperties() {
    rand = ThreadLocalRandom.current();
  }

  @Override
  public void onConfigReload() {
    CommentedConfigurationNode revertNode = config.node("properties", "revert-time");

    earthRevertTime = revertNode.node("earth").getLong(300_000);
    fireRevertTime = revertNode.node("fire").getLong(10_000);
    explosionRevertTime = revertNode.node("explosion").getLong(20_000);
    iceRevertTime = revertNode.node("ice").getLong(10_000);

    explosionKnockback = config.node("properties", "explosion-knockback").getDouble(0.8);

    CommentedConfigurationNode modifierNode = config.node("properties", "modifiers");

    metalModifier = modifierNode.node("metal").getDouble(1.25);
    magmaModifier = modifierNode.node("magma").getDouble(1.4);

    moonModifier = modifierNode.node("moon").getDouble(1.25);
    sunModifier = modifierNode.node("sun").getDouble(1.25);
  }

  public long earthRevertTime() {
    return earthRevertTime;
  }

  public long earthRevertTime(long delay) {
    return earthRevertTime + rand.nextLong(delay);
  }

  public long fireRevertTime() {
    return fireRevertTime;
  }

  public long fireRevertTime(long delay) {
    return fireRevertTime + rand.nextLong(delay);
  }

  public long explosionRevertTime() {
    return explosionRevertTime;
  }

  public long explosionRevertTime(long delay) {
    return explosionRevertTime + rand.nextLong(delay);
  }

  public long iceRevertTime() {
    return iceRevertTime;
  }

  public long iceRevertTime(long delay) {
    return iceRevertTime + rand.nextLong(delay);
  }

  public double explosionKnockback() {
    return explosionKnockback;
  }

  public double explosionKnockback(double value) {
    return value * explosionKnockback;
  }

  public double metalModifier() {
    return metalModifier;
  }

  public double metalModifier(double value) {
    return value * metalModifier;
  }

  public double magmaModifier() {
    return magmaModifier;
  }

  public double magmaModifier(double value) {
    return value * magmaModifier;
  }

  public double moonModifier() {
    return moonModifier;
  }

  public double moonModifier(double value) {
    return value * moonModifier;
  }

  public double sunModifier() {
    return sunModifier;
  }

  public double sunModifier(double value) {
    return value * sunModifier;
  }
}
