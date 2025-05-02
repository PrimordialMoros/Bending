/*
 * Copyright 2020-2025 Moros
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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.AbstractSpout;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.common.ability.SpoutAbility;
import me.moros.math.Vector3d;

public class AirSpout extends AbilityInstance implements SpoutAbility {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private AbstractSpout spout;

  public AirSpout(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).destroyUserInstances(user, AirSpout.class)) {
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
    removalPolicy = Policies.defaults();
    spout = new Spout();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
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
  public Collection<Collider> colliders() {
    return List.of(spout.collider());
  }

  @Override
  public void handleMovement(Vector3d velocity) {
    if (spout != null) {
      spout.limitVelocity(velocity, userConfig.maxSpeed);
    }
  }

  private final class Spout extends AbstractSpout {
    private long nextRenderTime;

    private Spout() {
      super(user, userConfig.height);
      nextRenderTime = 0;
    }

    @Override
    public void render(Vector3d location) {
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
    public void postRender(Vector3d location) {
      if (ThreadLocalRandom.current().nextInt(8) == 0) {
        SoundEffect.AIR.play(user.world(), location);
      }
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2000;
    @Modifiable(Attribute.HEIGHT)
    private double height = 11;
    @Modifiable(Attribute.SPEED)
    private double maxSpeed = 0.2;

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "airspout");
    }
  }
}
