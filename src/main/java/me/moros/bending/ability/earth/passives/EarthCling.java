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

package me.moros.bending.ability.earth.passives;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class EarthCling extends AbilityInstance implements Ability {
  private static final BlockData STONE = Material.STONE.createBlockData();
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  public EarthCling(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description()) || user.isOnGround()) {
      return UpdateResult.CONTINUE;
    }
    if (!user.selectedAbilityName().equals("EarthGlove")) {
      return UpdateResult.CONTINUE;
    }
    if (EntityMethods.isAgainstWall(user.entity(), b -> EarthMaterials.isEarthbendable(user, b) && !b.isLiquid())) {
      //noinspection ConstantConditions
      if (!user.onCooldown(user.selectedAbility())) {
        EntityMethods.applyVelocity(this, user.entity(), Vector3d.ZERO);
        user.entity().setFallDistance(0);
      } else {
        if (user.velocity().getY() < 0) {
          float fallDistance = Math.max(0, user.entity().getFallDistance() - (float) userConfig.speed);
          EntityMethods.applyVelocity(this, user.entity(), user.velocity().multiply(userConfig.speed));
          user.entity().setFallDistance(fallDistance);
          ParticleUtil.create(Particle.CRIT, user.entity().getEyeLocation()).count(2)
            .offset(0.05, 0.4, 0.05);
          ParticleUtil.create(Particle.BLOCK_CRACK, user.entity().getEyeLocation()).count(3)
            .offset(0.1, 0.4, 0.1).data(STONE);
        }
      }
    }
    return UpdateResult.CONTINUE;
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.SPEED)
    public double speed;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "passives", "earthcling");

      speed = abilityNode.node("speed").getDouble(0.5);
    }
  }
}

