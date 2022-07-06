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

import me.moros.bending.config.Configurable;
import me.moros.bending.model.properties.BendingProperties;
import org.spongepowered.configurate.CommentedConfigurationNode;

final class BendingPropertiesImpl extends Configurable implements BendingProperties {
  private long earthRevertTime;
  private long fireRevertTime;
  private long explosionRevertTime;
  private long iceRevertTime;

  private double explosionKnockback;

  private double metalModifier;
  private double magmaModifier;
  private double moonModifier;
  private double sunModifier;

  private boolean generateLight;

  BendingPropertiesImpl() {
  }

  @Override
  public void onConfigReload() {
    CommentedConfigurationNode revertNode = config.node("properties", "revert-time");

    earthRevertTime = revertNode.node("earth").getLong(BendingProperties.super.earthRevertTime());
    fireRevertTime = revertNode.node("fire").getLong(BendingProperties.super.fireRevertTime());
    explosionRevertTime = revertNode.node("explosion").getLong(BendingProperties.super.explosionRevertTime());
    iceRevertTime = revertNode.node("ice").getLong(BendingProperties.super.iceRevertTime());

    explosionKnockback = config.node("properties", "explosion-knockback").getDouble(BendingProperties.super.explosionKnockback());

    CommentedConfigurationNode modifierNode = config.node("properties", "modifiers");

    metalModifier = modifierNode.node("metal").getDouble(BendingProperties.super.metalModifier());
    magmaModifier = modifierNode.node("magma").getDouble(BendingProperties.super.magmaModifier());

    moonModifier = modifierNode.node("moon").getDouble(BendingProperties.super.moonModifier());
    sunModifier = modifierNode.node("sun").getDouble(BendingProperties.super.sunModifier());

    generateLight = config.node("properties", "generate-light").getBoolean(BendingProperties.super.canGenerateLight());
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
