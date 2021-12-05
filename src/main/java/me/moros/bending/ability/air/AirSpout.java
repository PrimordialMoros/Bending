/*
 * Copyright 2020-2021 Moros
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

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractSpout;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.EntityUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class AirSpout extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private AbstractSpout spout;

  public AirSpout(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).destroyInstanceType(user, AirSpout.class)) {
      return false;
    }
    if (Policies.IN_LIQUID.test(user, description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    double h = userConfig.height + 2;
    if (EntityUtil.distanceAboveGround(user.entity()) > h) {
      return false;
    }
    if (AbstractSpout.blockCast(user.locBlock(), h) == null) {
      return false;
    }
    removalPolicy = Policies.builder().build();
    spout = new Spout();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description()) || user.headBlock().isLiquid()) {
      return UpdateResult.REMOVE;
    }

    return spout.update();
  }

  @Override
  public void onDestroy() {
    spout.flight().flying(false);
    spout.flight().release();
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(spout.collider());
  }

  public void handleMovement(@NonNull Vector3d velocity) {
    AbstractSpout.limitVelocity(user.entity(), velocity, userConfig.maxSpeed);
  }

  private class Spout extends AbstractSpout {
    private long nextRenderTime;

    public Spout() {
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
        ParticleUtil.air(user.entity().getLocation().subtract(0, i, 0))
          .count(3).offset(0.4, 0.4, 0.4).spawn();
      }
      nextRenderTime = time + 100;
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(8) == 0) {
        SoundUtil.AIR.play(user.entity().getLocation());
      }
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.HEIGHT)
    public double height;
    @Modifiable(Attribute.SPEED)
    public double maxSpeed;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airspout");

      cooldown = abilityNode.node("cooldown").getLong(2000);
      height = abilityNode.node("height").getDouble(11.0);
      maxSpeed = abilityNode.node("max-speed").getDouble(0.2);
    }
  }
}
