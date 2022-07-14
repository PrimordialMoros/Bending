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

package me.moros.bending.ability.earth;

import java.util.List;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.basic.AbstractRide;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.temporal.TempEntity.Builder;
import me.moros.bending.temporal.TempEntity.TempEntityType;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class EarthSurf extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
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
    if (user.game().abilityManager(user.world()).hasAbility(user, EarthSurf.class)) {
      return false;
    }
    this.user = user;
    loadConfig();
    charging = true;
    if (method == Activation.FALL) {
      if (user.entity().getFallDistance() < userConfig.fallThreshold || user.sneaking()) {
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
    double dist = EntityUtil.distanceAboveGround(user.entity(), 2.5);
    Block check = user.location().subtract(0, dist + 0.05, 0).toBlock(user.world());
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
    if (!user.canBuild(user.locBlock())) {
      return UpdateResult.REMOVE;
    }
    if (charging) {
      if (System.currentTimeMillis() >= startTime + userConfig.chargeTime) {
        if (user.sneaking()) {
          ParticleUtil.of(Particle.SMOKE_NORMAL, user.mainHandSide()).spawn(user.world());
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

  private final class Wave extends AbstractRide {
    private Vector3d center;
    private int ticks = 0;

    private Wave() {
      super(user, userConfig.speed, 2.25);
      predicate = EarthMaterials::isEarthOrSand;
    }

    @Override
    public void render(BlockData data) {
      Builder builder = TempEntity.builder(MaterialUtil.softType(data)).velocity(new Vector3d(0, 0.25, 0)).duration(500);
      Vector3d center = user.location().add(Vector3d.MINUS_J);
      Vector3d dir = user.direction().withY(0).normalize(user.velocity().withY(0).normalize());
      VectorUtil.createArc(dir, Vector3d.PLUS_J, Math.PI / 3, 3).forEach(v -> {
        Vector3d point = center.add(v.multiply(0.6));
        builder.build(TempEntityType.FALLING_BLOCK, user.world(), point);
      });
    }

    @Override
    public void postRender() {
      center = user.location().subtract(0, 0.5, 0);
      if (++ticks % 4 == 0) {
        SoundUtil.of(Sound.BLOCK_ROOTED_DIRT_FALL, 0.6F, 0).play(user.world(), center);
      }
      CollisionUtil.handle(user, new Sphere(center, 1.2), this::onEntityHit, false);
    }

    @Override
    protected void affect(Vector3d velocity) {
      EntityUtil.applyVelocity(EarthSurf.this, user.entity(), velocity);
    }

    private boolean onEntityHit(Entity entity) {
      Vector3d velocity = EntityUtil.entityCenter(entity).subtract(center).withY(0.35).normalize();
      EntityUtil.applyVelocity(EarthSurf.this, entity, velocity);
      return false;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
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
    public Iterable<String> path() {
      return List.of("abilities", "earth", "earthsurf");
    }
  }
}
