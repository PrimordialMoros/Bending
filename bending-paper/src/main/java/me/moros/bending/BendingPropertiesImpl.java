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

import java.util.List;

import me.moros.bending.config.Configurable;
import me.moros.bending.model.properties.BendingProperties;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
final class BendingPropertiesImpl extends Configurable implements BendingProperties {
  private long earthRevertTime = BendingProperties.super.earthRevertTime();
  private long fireRevertTime = BendingProperties.super.fireRevertTime();
  private long explosionRevertTime = BendingProperties.super.explosionRevertTime();
  private long iceRevertTime = BendingProperties.super.iceRevertTime();

  private double explosionKnockback = BendingProperties.super.explosionKnockback();

  private double metalModifier = BendingProperties.super.metalModifier();
  private double magmaModifier = BendingProperties.super.magmaModifier();
  private double moonModifier = BendingProperties.super.moonModifier();
  private double sunModifier = BendingProperties.super.sunModifier();

  private boolean generateLight = BendingProperties.super.canGenerateLight();

  BendingPropertiesImpl() {
  }

  @Override
  public Iterable<String> path() {
    return List.of("properties");
  }

  @Override
  public long earthRevertTime() {
    return earthRevertTime;
  }

  @Override
  public long fireRevertTime() {
    return fireRevertTime;
  }

  @Override
  public long explosionRevertTime() {
    return explosionRevertTime;
  }

  @Override
  public long iceRevertTime() {
    return iceRevertTime;
  }

  @Override
  public double explosionKnockback() {
    return explosionKnockback;
  }

  @Override
  public double metalModifier() {
    return metalModifier;
  }

  @Override
  public double magmaModifier() {
    return magmaModifier;
  }

  @Override
  public double moonModifier() {
    return moonModifier;
  }

  @Override
  public double sunModifier() {
    return sunModifier;
  }

  @Override
  public boolean canGenerateLight() {
    return generateLight;
  }
}
