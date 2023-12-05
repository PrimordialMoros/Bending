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

package me.moros.bending.common.ability.earth;

import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.AbstractRide;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.temporal.TempDisplayEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthSurf extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Wave wave;

  private boolean charging;
  private long startTime;

  public EarthSurf(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, EarthSurf.class)) {
      return false;
    }
    this.user = user;
    loadConfig();
    charging = true;
    if (method == Activation.FALL) {
      if (user.fallDistance() < userConfig.fallThreshold || user.sneaking()) {
        return false;
      }
      return launch();
    }
    startTime = System.currentTimeMillis();
    removalPolicy = Policies.builder()
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();
    return true;
  }

  private boolean launch() {
    double dist = user.distanceAboveGround(2.5);
    Block check = user.world().blockAt(user.location().subtract(0, dist + 0.05, 0));
    if (dist > 2.25 || !EarthMaterials.isEarthOrSand(check)) {
      return false;
    }
    charging = false;
    removalPolicy = Policies.builder()
      .add(Policies.SNEAKING)
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();
    wave = new Wave();
    return true;
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
    if (charging) {
      if (System.currentTimeMillis() >= startTime + userConfig.chargeTime) {
        if (user.sneaking()) {
          Particle.SMOKE.builder(user.mainHandSide()).spawn(user.world());
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
      wave.onDestroy();
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  private final class Wave extends AbstractRide {
    private Vector3d center;
    private int ticks = 0;

    private Wave() {
      super(user, userConfig.speed, 2.25);
      predicate = EarthMaterials::isEarthOrSand;
    }

    @Override
    public void render(BlockState data) {
      if (ticks % 3 == 0) {
        return;
      }
      var builder = TempDisplayEntity.builder(data).gravity(true).velocity(Vector3d.of(0, 0.25, 0))
        .minYOffset(-1.25).duration(750);
      Vector3d center = user.location().add(Vector3d.MINUS_J);
      Vector3d dir = user.direction().withY(0).normalize(user.velocity().withY(0).normalize());
      VectorUtil.createArc(dir, Vector3d.PLUS_J, Math.PI / 3, 3).forEach(v ->
        builder.build(user.world(), center.add(v.multiply(0.6)))
      );
    }

    @Override
    public void postRender() {
      center = user.location().subtract(0, 0.5, 0);
      if (++ticks % 4 == 0) {
        Sound.BLOCK_ROOTED_DIRT_FALL.asEffect(0.6F, 0).play(user.world(), center);
      }
      CollisionUtil.handle(user, Sphere.of(center, 1.2), this::onEntityHit, false);
    }

    @Override
    protected void affect(Vector3d velocity) {
      user.applyVelocity(EarthSurf.this, velocity);
    }

    private boolean onEntityHit(Entity entity) {
      Vector3d velocity = entity.center().subtract(center).withY(0.35).normalize();
      entity.applyVelocity(EarthSurf.this, velocity);
      return false;
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.SPEED)
    private double speed = 0.5;
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.CHARGE_TIME)
    private long chargeTime = 1500;
    @Modifiable(Attribute.DURATION)
    private long duration = 0;
    private double fallThreshold = 12;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "earthsurf");
    }
  }
}
