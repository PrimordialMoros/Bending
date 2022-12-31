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

package me.moros.bending.ability.air;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.basic.AbstractSpout;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.particle.ParticleBuilder;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirSpout extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private AbstractSpout spout;

  public AirSpout(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldUid()).destroyUserInstance(user, AirSpout.class)) {
      return false;
    }
    if (Policies.UNDER_WATER.test(user, description()) || Policies.UNDER_LAVA.test(user, description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    double h = userConfig.height + 2;
    if (AbstractSpout.blockCast(user.block(), h) == null) {
      return false;
    }
    removalPolicy = Policies.builder().build();
    spout = new Spout();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || user.eyeBlock().type().isLiquid()) {
      return UpdateResult.REMOVE;
    }

    return spout.update();
  }

  @Override
  public void onDestroy() {
    spout.onDestroy();
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(spout.collider());
  }

  public void handleMovement(Vector3d velocity) {
    AbstractSpout.limitVelocity(user.entity(), velocity, userConfig.maxSpeed);
  }

  private final class Spout extends AbstractSpout {
    private long nextRenderTime;

    private Spout() {
      super(user, userConfig.height);
      nextRenderTime = 0;
    }

    @Override
    public void render() {
      long time = System.currentTimeMillis();
      if (time < nextRenderTime) {
        return;
      }
      for (int i = 0; i < distance; i++) {
        ParticleBuilder.air(user.location().subtract(0, i, 0)).count(3).offset(0.4).spawn(user.world());
      }
      nextRenderTime = time + 100;
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(8) == 0) {
        SoundEffect.AIR.play(user.world(), user.location());
      }
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2000;
    @Modifiable(Attribute.HEIGHT)
    private double height = 11;
    @Modifiable(Attribute.SPEED)
    private double maxSpeed = 0.2;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "air", "airspout");
    }
  }
}
