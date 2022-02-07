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

package me.moros.bending.ability.air;

import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.air.sequence.AirWheel;
import me.moros.bending.ability.common.basic.AbstractRide;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class AirScooter extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Scooter scooter;

  public boolean canRender = true;

  public AirScooter(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, AirScooter.class)) {
      return false;
    }
    if (Bending.game().abilityManager(user.world()).hasAbility(user, AirWheel.class)) {
      return false;
    }
    this.user = user;
    loadConfig();

    double dist = EntityUtil.distanceAboveGround(user.entity(), 3.5);
    if (dist < 0.5 || dist > 3.25) {
      return false;
    }
    removalPolicy = Policies.builder()
      .add(Policies.SNEAKING)
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();
    scooter = new Scooter();
    return removalPolicy.test(user, description());
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (!user.canBuild(user.locBlock())) {
      return UpdateResult.REMOVE;
    }
    return scooter.update();
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class Scooter extends AbstractRide {
    private double verticalPosition = 0;

    private Scooter() {
      super(user, userConfig.speed, 3.25);
    }

    @Override
    public void render(@NonNull BlockData data) {
      if (!canRender) {
        return;
      }
      verticalPosition += 0.25 * Math.PI;
      Vector3d location = user.location();
      for (double theta = 0; theta < 2 * Math.PI * 2; theta += Math.PI / 5) {
        double sin = Math.sin(verticalPosition);
        double x = 0.6 * Math.cos(theta) * sin;
        double y = 0.6 * Math.cos(verticalPosition);
        double z = 0.6 * Math.sin(theta) * sin;
        ParticleUtil.air(location.add(new Vector3d(x, y - 0.25, z))).spawn(user.world());
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        SoundUtil.AIR.play(user.world(), user.location());
      }
    }

    @Override
    protected void affect(@NonNull Vector3d velocity) {
      EntityUtil.applyVelocity(AirScooter.this, user.entity(), velocity);
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DURATION)
    public long duration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airscooter");

      speed = abilityNode.node("speed").getDouble(0.7);
      cooldown = abilityNode.node("cooldown").getLong(2000);
      duration = abilityNode.node("duration").getLong(15000);
    }
  }
}
