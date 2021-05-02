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

package me.moros.bending.ability.air.passives;

import me.moros.bending.Bending;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.user.User;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GracefulDescent extends AbilityInstance implements Ability {
  private User user;

  public GracefulDescent(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();
    return true;
  }

  @Override
  public void recalculateConfig() {
  }

  @Override
  public @NonNull UpdateResult update() {
    return UpdateResult.CONTINUE;
  }

  public static boolean isGraceful(User user) {
    if (!Bending.game().abilityManager(user.world()).hasAbility(user, GracefulDescent.class)) {
      return false;
    }
    return Bending.game().abilityRegistry().abilityDescription("GracefulDescent").map(user::canBend).orElse(false);
  }

  @Override
  public @NonNull User user() {
    return user;
  }
}
