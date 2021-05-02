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

package me.moros.bending.ability.fire;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.fire.sequences.JetBlast;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.Flight;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class FireJet extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Flight flight;

  private double speed;
  private long duration;
  private long startTime;

  public FireJet(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (method == ActivationMethod.ATTACK) {
      if (Bending.game().abilityManager(user.world()).destroyInstanceType(user, FireJet.class)) {
        return false;
      }
      if (Bending.game().abilityManager(user.world()).destroyInstanceType(user, JetBlast.class)) {
        return false;
      }
    }

    this.user = user;
    recalculateConfig();

    Block block = user.locBlock();
    boolean ignitable = MaterialUtil.isIgnitable(block);
    if (!ignitable && !MaterialUtil.isAir(block)) {
      return false;
    }

    speed = userConfig.speed;
    duration = userConfig.duration;

    flight = Flight.get(user);
    if (ignitable) {
      TempBlock.create(block, Material.FIRE.createBlockData(), BendingProperties.FIRE_REVERT_TIME, true);
    }

    removalPolicy = Policies.builder()
      .add(Policies.IN_LIQUID)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();

    FireTick.extinguish(user.entity());
    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    // scale down to 0.5 speed near the end
    double factor = 1 - ((System.currentTimeMillis() - startTime) / (2.0 * duration));

    user.entity().setVelocity(user.direction().scalarMultiply(speed * factor).toVector());
    user.entity().setFallDistance(0);
    ParticleUtil.createFire(user, user.entity().getLocation()).count(10)
      .offset(0.3, 0.3, 0.3).extra(0.03).spawn();

    return UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
    flight.release();
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  public void speed(double speed) {
    this.speed = speed;
  }

  public void duration(long duration) {
    this.duration = duration;
    removalPolicy = Policies.builder().add(Policies.IN_LIQUID).add(ExpireRemovalPolicy.of(duration)).build();
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.SPEED)
    public double speed;
    @Attribute(Attribute.DURATION)
    private long duration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "firejet");

      cooldown = abilityNode.node("cooldown").getLong(7000);
      speed = abilityNode.node("speed").getDouble(0.8);
      duration = abilityNode.node("duration").getLong(2000);
    }
  }
}
