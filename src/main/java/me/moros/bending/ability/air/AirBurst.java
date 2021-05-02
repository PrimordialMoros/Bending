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

package me.moros.bending.ability.air;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractBurst;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;

public class AirBurst extends AbstractBurst {
  private enum Mode {CONE, SPHERE, FALL}

  private static final Config config = new Config();

  private User user;
  private Config userConfig;

  private boolean released;
  private long startTime;

  public AirBurst(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (method == ActivationMethod.ATTACK) {
      Bending.game().abilityManager(user.world()).firstInstance(user, AirBurst.class)
        .ifPresent(b -> b.release(Mode.CONE));
      return false;
    }

    this.user = user;
    recalculateConfig();

    released = false;
    if (method == ActivationMethod.FALL) {
      if (user.entity().getFallDistance() < userConfig.fallThreshold || user.sneaking()) {
        return false;
      }
      release(Mode.FALL);
    }
    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (!released) {
      boolean charged = isCharged();
      if (charged) {
        ParticleUtil.createAir(user.mainHandSide().toLocation(user.world())).spawn();
        if (!user.sneaking()) {
          release(Mode.SPHERE);
        }
      } else {
        if (!user.sneaking()) {
          return UpdateResult.REMOVE;
        }
      }
      return UpdateResult.CONTINUE;
    }
    return updateBurst();
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private boolean isCharged() {
    return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
  }

  private void release(Mode mode) {
    if (released || !isCharged()) {
      return;
    }
    released = true;
    switch (mode) {
      case CONE:
        cone(() -> new AirBlast(description()), userConfig.coneRange);
      case FALL:
        fall(() -> new AirBlast(description()), userConfig.sphereRange);
      case SPHERE:
      default:
        sphere(() -> new AirBlast(description()), userConfig.sphereRange);
    }
    user.addCooldown(description(), userConfig.cooldown);
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.CHARGE_TIME)
    public int chargeTime;
    @Attribute(Attribute.RANGE)
    public double sphereRange;
    @Attribute(Attribute.RANGE)
    public double coneRange;
    public double fallThreshold;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airburst");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      chargeTime = abilityNode.node("charge-time").getInt(3500);
      coneRange = abilityNode.node("cone-range").getDouble(16.0);
      sphereRange = abilityNode.node("sphere-range").getDouble(12.0);
      fallThreshold = abilityNode.node("fall-threshold").getDouble(14.0);
    }
  }
}
