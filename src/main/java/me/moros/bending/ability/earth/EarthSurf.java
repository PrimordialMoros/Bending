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


import java.util.Collection;
import java.util.HashSet;

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
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.packet.PacketUtil;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class EarthSurf extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Wave wave;
  private final Collection<Block> blockBuffer = new HashSet<>();

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
    if (method == Activation.FALL) {
      if (user.entity().getFallDistance() < userConfig.fallThreshold || user.sneaking()) {
        return false;
      }
      return launch();
    }
    startTime = System.currentTimeMillis();
    removalPolicy = Policies.builder()
      .add(Policies.IN_LIQUID)
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();
    return true;
  }

  private boolean launch() {
    double dist = EntityUtil.distanceAboveGround(user.entity(), 2.5);
    Block check = user.location().subtract(new Vector3d(0, dist + 0.05, 0)).toBlock(user.world());
    if (dist > 2.25 || !EarthMaterials.isEarthOrSand(check)) {
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
      PacketUtil.refreshBlocks(blockBuffer, user.world(), user.location());
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class Wave extends AbstractRide {
    private Vector3d center;
    private int ticks = 0;

    private Wave() {
      super(user, userConfig.speed, 2.25);
      predicate = EarthMaterials::isEarthOrSand;
    }

    @Override
    public void render(@NonNull BlockData data) {
      if (++ticks % 2 == 0) {
        if (ticks % 4 == 0) {
          PacketUtil.refreshBlocks(blockBuffer, user.world(), user.location());
          blockBuffer.clear();
        }
        return;
      }
      Builder builder = TempPacketEntity.builder(MaterialUtil.softType(data)).velocity(new Vector3d(0, 0.25, 0)).duration(500);
      Vector3d center = user.location().add(Vector3d.MINUS_J);
      toRefresh(user.locBlock());
      toRefresh(center.toBlock(user.world()));
      Vector3d dir = user.direction().setY(0).normalize(user.velocity().setY(0).normalize());
      VectorUtil.createArc(dir, Vector3d.PLUS_J, Math.PI / 3, 3).forEach(v -> {
        Vector3d point = center.add(v.multiply(0.6));
        toRefresh(point.toBlock(user.world()));
        builder.buildFallingBlock(user.world(), point);
      });
    }

    private void toRefresh(Block block) {
      if (!MaterialUtil.isTransparent(block)) {
        blockBuffer.add(block);
      }
    }

    @Override
    public void postRender() {
      center = user.location().subtract(new Vector3d(0, 0.5, 0));
      CollisionUtil.handle(user, new Sphere(center, 1.2), this::onEntityHit, false);
    }

    @Override
    protected void affect(@NonNull Vector3d velocity) {
      EntityUtil.applyVelocity(EarthSurf.this, user.entity(), velocity);
    }

    private boolean onEntityHit(Entity entity) {
      Vector3d velocity = EntityUtil.entityCenter(entity).subtract(center).setY(0.35).normalize();
      EntityUtil.applyVelocity(EarthSurf.this, entity, velocity);
      return false;
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
    public double fallThreshold;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthsurf");

      speed = abilityNode.node("speed").getDouble(0.5);
      cooldown = abilityNode.node("cooldown").getLong(6000);
      chargeTime = abilityNode.node("charge-time").getLong(1500);
      duration = abilityNode.node("duration").getLong(0);
      fallThreshold = abilityNode.node("fall-threshold").getDouble(12.0);
    }
  }
}
