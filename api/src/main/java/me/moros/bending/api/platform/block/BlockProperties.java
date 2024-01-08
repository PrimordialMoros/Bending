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

package me.moros.bending.api.platform.block;

import me.moros.bending.api.platform.sound.SoundGroup;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.kyori.adventure.translation.Translatable;

public interface BlockProperties extends Keyed, Translatable {
  boolean isAir();

  boolean isSolid();

  boolean isLiquid();

  boolean isFlammable();

  boolean hasGravity();

  boolean isCollidable(); // TODO some blocks can conditionally not have a collider based on their state, blocktype isn't enough

  double hardness();

  SoundGroup soundGroup();

  final class Builder {
    final Key key;
    final String translationKey;
    boolean isAir;
    boolean isSolid;
    boolean isLiquid;
    boolean isFlammable;
    boolean hasGravity;
    boolean isCollidable;
    double hardness;
    SoundGroup soundGroup;

    private Builder(Key key, String translationKey) {
      this.key = key;
      this.translationKey = translationKey;
    }

    public Builder isAir(boolean isAir) {
      this.isAir = isAir;
      return this;
    }

    public Builder isSolid(boolean isSolid) {
      this.isSolid = isSolid;
      return this;
    }

    public Builder isLiquid(boolean isLiquid) {
      this.isLiquid = isLiquid;
      return this;
    }

    public Builder isFlammable(boolean isFlammable) {
      this.isFlammable = isFlammable;
      return this;
    }

    public Builder hasGravity(boolean hasGravity) {
      this.hasGravity = hasGravity;
      return this;
    }

    public Builder isCollidable(boolean isCollidable) {
      this.isCollidable = isCollidable;
      return this;
    }

    public Builder hardness(double hardness) {
      this.hardness = hardness;
      return this;
    }

    public Builder soundGroup(SoundGroup soundGroup) {
      this.soundGroup = soundGroup;
      return this;
    }

    public BlockProperties build() {
      return new BlockPropertiesImpl(this);
    }
  }

  static Builder builder(BlockType type, String translationKey) {
    return new Builder(type.key(), translationKey);
  }
}
