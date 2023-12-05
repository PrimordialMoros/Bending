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

package me.moros.bending.common.config;

import java.util.List;

import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
public final class BendingPropertiesImpl implements BendingProperties, Configurable {
  private long earthRevertTime = Holder.DEFAULTS.earthRevertTime();
  private long fireRevertTime = Holder.DEFAULTS.fireRevertTime();
  private long explosionRevertTime = Holder.DEFAULTS.explosionRevertTime();
  private long iceRevertTime = Holder.DEFAULTS.iceRevertTime();

  private double explosionKnockback = Holder.DEFAULTS.explosionKnockback();

  private double metalModifier = Holder.DEFAULTS.metalModifier();
  private double magmaModifier = Holder.DEFAULTS.magmaModifier();
  private double moonModifier = Holder.DEFAULTS.moonModifier();
  private double sunModifier = Holder.DEFAULTS.sunModifier();

  private boolean generateLight = Holder.DEFAULTS.canGenerateLight();
  private boolean lazyLoad = Holder.DEFAULTS.lazyLoad();

  private int maxPresets = Holder.DEFAULTS.maxPresets();

  @Override
  public List<String> path() {
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

  @Override
  public boolean lazyLoad() {
    return lazyLoad;
  }

  @Override
  public int maxPresets() {
    return maxPresets;
  }
}
