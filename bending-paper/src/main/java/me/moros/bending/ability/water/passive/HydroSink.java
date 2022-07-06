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

import me.moros.bending.Bending;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class HydroSink extends AbilityInstance {
  private User user;

  public HydroSink(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    return true;
  }

  @Override
  public void loadConfig() {
  }

  @Override
  public @NonNull UpdateResult update() {
    return UpdateResult.CONTINUE;
  }

  public static boolean canHydroSink(@NonNull User user) {
    AbilityDescription desc = Registries.ABILITIES.ability("HydroSink");
    if (desc == null || !user.canBend(desc)) {
      return false;
    }

    if (!Bending.game().abilityManager(user.world()).hasAbility(user, HydroSink.class)) {
      return false;
    }

    AABB entityBounds = AABBUtil.entityBounds(user.entity()).grow(new Vector3d(0, 0.2, 0));
    AABB floorBounds = new AABB(new Vector3d(-1, -0.5, -1), new Vector3d(1, 0, 1)).at(user.location());
    return WorldUtil.nearbyBlocks(user.world(), floorBounds, b -> entityBounds.intersects(AABBUtil.blockBounds(b)))
      .stream().anyMatch(WaterMaterials::isWaterBendable);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }
}
