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

package me.moros.bending.ability.water.passive;

import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class FastSwim extends AbilityInstance {

  private User user;
  private RemovalPolicy removalPolicy;

  public FastSwim(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    removalPolicy = Policies.builder().add((u, d) -> !u.entity().isInWaterOrBubbleColumn()).add(Policies.FLYING).build();
    return true;
  }

  @Override
  public void loadConfig() {
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description())) {
      user.entity().removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
      return UpdateResult.CONTINUE;
    }
    EntityUtil.tryAddPotion(user.entity(), PotionEffectType.DOLPHINS_GRACE, 100, 0);
    return UpdateResult.CONTINUE;
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }
}
