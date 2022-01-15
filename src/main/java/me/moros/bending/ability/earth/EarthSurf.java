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

package me.moros.bending.ability.earth;


import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.AbstractRide;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempPacketEntity;
import me.moros.bending.game.temporal.TempPacketEntity.Builder;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.material.EarthMaterials;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class EarthSurf extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Wave wave;

  private boolean charging;
  private long startTime;

  public EarthSurf(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, EarthSurf.class)) {
      return false;
    }
    this.user = user;
    loadConfig();
    charging = true;
    startTime = System.currentTimeMillis();
    removalPolicy = Policies.builder()
      .add(Policies.IN_LIQUID)
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();
    return true;
  }

  private boolean launch() {
    double dist = EntityUtil.distanceAboveGround(user.entity(), 2.5);
    if (dist > 2.25) {
      return false;
    }
    charging = false;
    removalPolicy = Policies.builder()
      .add(Policies.SNEAKING)
      .add(Policies.IN_LIQUID)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();
    wave = new Wave();
    return true;
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
    if (charging) {
      if (System.currentTimeMillis() >= startTime + userConfig.chargeTime) {
        if (user.sneaking()) {
          ParticleUtil.of(Particle.SMOKE_NORMAL, user.mainHandSide().toLocation(user.world())).spawn();
          return UpdateResult.CONTINUE;
        } else {
          return launch() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
        }
      } else if (user.sneaking()) {
        return UpdateResult.CONTINUE;
      }
      return UpdateResult.REMOVE;
    }
    return wave.update();
  }

  @Override
  public void onDestroy() {
    if (wave != null) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class Wave extends AbstractRide {
    private int tick = 0;

    private Wave() {
      super(user, userConfig.speed, 2.25);
      predicate = b -> EarthMaterials.isEarthbendable(user, b);
    }

    @Override
    public void render() {
      if (++tick % 2 == 0) {
        return;
      }
      Builder builder = TempPacketEntity.builder(Material.DIRT.createBlockData())
        .velocity(new Vector3d(0, 0.2, 0)).duration(500);
      Vector3d center = user.location().add(Vector3d.MINUS_J);
      VectorUtil.createArc(user.direction().setY(0), Vector3d.PLUS_J, Math.PI / 3, 3).forEach(v ->
        builder.buildFallingBlock(user.world(), center.add(v.multiply(0.6)))
      );
    }

    @Override
    public void postRender() {
    }

    @Override
    protected void affect(@NonNull Vector3d velocity) {
      EntityUtil.applyVelocity(EarthSurf.this, user.entity(), velocity);
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.CHARGE_TIME)
    public long chargeTime;
    @Modifiable(Attribute.DURATION)
    public long duration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthsurf");

      speed = abilityNode.node("speed").getDouble(0.5);
      cooldown = abilityNode.node("cooldown").getLong(6000);
      chargeTime = abilityNode.node("charge-time").getLong(1500);
      duration = abilityNode.node("duration").getLong(0);
    }
  }
}
