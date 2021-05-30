/*
 *   Copyright 2020-2021 Moros <https://github.com/PrimordialMoros>
 *
 *    This file is part of Bending.
 *
 *   Bending is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Bending is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with Bending.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.moros.bending.model.ability;

import org.checkerframework.checker.nullness.qual.NonNull;

public enum ActivationMethod {
  PASSIVE("bending.activation.passive"),
  ATTACK("bending.activation.attack"),
  ATTACK_ENTITY("bending.activation.attack-entity"),
  INTERACT("bending.activation.interact", true),
  INTERACT_ENTITY("bending.activation.interact-entity", true),
  INTERACT_BLOCK("bending.activation.interact-block", true),
  SNEAK("bending.activation.sneak"),
  SNEAK_RELEASE("bending.activation.sneak-release"),
  FALL("bending.activation.fall"),
  SEQUENCE("bending.activation.sequence");

  private final String key;
  private final boolean interact;

  ActivationMethod(String key) {
    this(key, false);
  }

  ActivationMethod(String key, boolean interact) {
    this.key = key;
    this.interact = interact;
  }

  public boolean isInteract() {
    return interact;
  }

  public @NonNull String key() {
    return key;
  }
}
