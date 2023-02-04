/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.common.ability.air;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.AbstractRide;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.common.ability.air.sequence.AirWheel;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirScooter extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Scooter scooter;

  private final boolean canRender;

  public AirScooter(AbilityDescription desc) {
    super(desc);
    this.canRender = true;
  }

  public AirScooter(AbilityDescription desc, boolean canRender) {
    super(desc);
    this.canRender = canRender;
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, AirScooter.class)) {
      return false;
    }
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, AirWheel.class)) {
      return false;
    }
    this.user = user;
    loadConfig();

    double dist = user.distanceAboveGround(3.5);
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
    return !removalPolicy.test(user, description());
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (!user.canBuild()) {
      return UpdateResult.REMOVE;
    }
    return scooter.update();
  }

  @Override
  public void onDestroy() {
    scooter.onDestroy();
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private final class Scooter extends AbstractRide {
    private double verticalPosition = 0;

    private Scooter() {
      super(user, userConfig.speed, 3.25);
    }

    @Override
    public void render(BlockState data) {
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
        ParticleBuilder.air(location.add(x, y - 0.25, z)).spawn(user.world());
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(4) == 0) {
        SoundEffect.AIR.play(user.world(), user.location());
      }
    }

    @Override
    protected void affect(Vector3d velocity) {
      user.applyVelocity(AirScooter.this, velocity);
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.SPEED)
    private double speed = 0.7;
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2000;
    @Modifiable(Attribute.DURATION)
    private long duration = 15000;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "air", "airscooter");
    }
  }
}
