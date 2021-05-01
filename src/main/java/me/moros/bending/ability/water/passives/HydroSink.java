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

package me.moros.bending.ability.water.passives;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.bending.Bending;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.user.User;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.block.Block;

public class HydroSink extends AbilityInstance implements PassiveAbility {
  private User user;

  public HydroSink(@NonNull AbilityDescription desc) {
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

  public static boolean canHydroSink(User user) {
    if (!Bending.game().abilityRegistry().abilityDescription("HydroSink").map(user::canBend).orElse(false)) {
      return false;
    }

    if (!Bending.game().abilityManager(user.world()).hasAbility(user, HydroSink.class)) {
      return false;
    }

    AABB entityBounds = AABBUtils.entityBounds(user.entity()).at(new Vector3(0, -0.5, 0));
    for (Block block : WorldMethods.nearbyBlocks(user.world(), entityBounds.grow(Vector3.HALF), WaterMaterials::isWaterBendable)) {
      if (block.getY() > entityBounds.position().getY()) {
        continue;
      }
      if (AABBUtils.blockBounds(block).intersects(entityBounds)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public @NonNull User user() {
    return user;
  }
}
