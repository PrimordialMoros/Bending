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

package me.moros.bending.common.ability.earth.passive;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.EntityUtil;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.potion.PotionEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.math.Vector3d;

public class EarthCling extends AbilityInstance {
  private RemovalPolicy removalPolicy;

  public EarthCling(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
    return true;
  }

  @Override
  public void loadConfig() {
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.hasAbilitySelected("earthglove") || user.isOnGround()) {
      return UpdateResult.CONTINUE;
    }
    if (EntityUtil.isAgainstWall(user, b -> EarthMaterials.isEarthbendable(user, b) && !b.type().isLiquid())) {
      EntityUtil.tryAddPotion(user, PotionEffect.SLOW_FALLING, 10, 0);
      //noinspection ConstantConditions
      if (!user.onCooldown(user.selectedAbility())) {
        user.applyVelocity(this, Vector3d.ZERO);
        user.fallDistance(0);
      } else {
        if (user.velocity().y() < 0) {
          Particle.CRIT.builder(user.eyeLocation()).count(2).offset(0.05, 0.4, 0.05).spawn(user.world());
          BlockType.STONE.asParticle(user.eyeLocation()).count(3).offset(0.1, 0.4, 0.1).spawn(user.world());
        }
      }
    }
    return UpdateResult.CONTINUE;
  }
}

