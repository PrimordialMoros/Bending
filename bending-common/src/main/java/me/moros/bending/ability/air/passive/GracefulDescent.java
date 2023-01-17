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

package me.moros.bending.ability.air.passive;

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class GracefulDescent extends AbilityInstance {
  private User user;

  public GracefulDescent(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    return true;
  }

  @Override
  public void loadConfig() {
  }

  @Override
  public UpdateResult update() {
    return UpdateResult.CONTINUE;
  }

  public static boolean isGraceful(User user) {
    return user.game().abilityManager(user.worldKey()).firstInstance(user, GracefulDescent.class)
      .map(a -> user.canBend(a.description())).orElse(false);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }
}
