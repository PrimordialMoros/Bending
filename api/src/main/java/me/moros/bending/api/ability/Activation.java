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

package me.moros.bending.api.ability;

import me.moros.bending.api.util.KeyUtil;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.Translatable;

/**
 * Represents a type of ability activation.
 */
public enum Activation implements Translatable {
  /**
   * Passive abilities are always active.
   */
  PASSIVE("passive"),
  /**
   * Activation on click.
   */
  ATTACK("attack"),
  /**
   * Activation on interaction.
   */
  INTERACT("interact"),
  /**
   * Activation on interaction with an entity.
   */
  INTERACT_ENTITY("interact-entity"),
  /**
   * Activation on interaction with a block.
   */
  INTERACT_BLOCK("interact-block"),
  /**
   * Activation on sneak.
   */
  SNEAK("sneak"),
  /**
   * Activation on sneak release.
   */
  SNEAK_RELEASE("sneak-release"),
  /**
   * Activations on fall damage.
   */
  FALL("fall"),
  /**
   * Activation by sequence.
   */
  SEQUENCE("sequence");

  private final Key key;

  Activation(String value) {
    this.key = KeyUtil.simple("activation." + value);
  }

  @Override
  public String translationKey() {
    return KeyUtil.concat(key);
  }
}
