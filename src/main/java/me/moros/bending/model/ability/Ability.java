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

import java.util.Collection;
import java.util.List;

import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public interface Ability extends Updatable {
  boolean activate(@NonNull User user, @NonNull ActivationMethod method); // return true if the ability was activated

  void recalculateConfig();

  @NonNull AbilityDescription description();

  @NonNull User user();

  default boolean user(@NonNull User newUser) {
    return false;
  }

  default @NonNull Collection<@NonNull Collider> colliders() {
    return List.of();
  }

  default void onCollision(@NonNull Collision collision) {
  }

  default void onDestroy() {
  }
}
