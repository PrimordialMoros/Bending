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

import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class HydroSink extends AbilityInstance {
  private User user;

  public HydroSink(AbilityDescription desc) {
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

  private boolean canHydroSink() {
    if (!user.canBend(description())) {
      return false;
    }
    AABB entityBounds = AABBUtil.entityBounds(user.entity()).grow(new Vector3d(0, 0.2, 0));
    AABB floorBounds = new AABB(new Vector3d(-1, -0.5, -1), new Vector3d(1, 0, 1)).at(user.location());
    return WorldUtil.nearbyBlocks(user.world(), floorBounds, b -> entityBounds.intersects(AABBUtil.blockBounds(b)))
      .stream().anyMatch(WaterMaterials::isWaterBendable);
  }

  public static boolean canHydroSink(User user) {
    return user.game().abilityManager(user.world()).firstInstance(user, HydroSink.class)
      .map(HydroSink::canHydroSink).orElse(false);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }
}
