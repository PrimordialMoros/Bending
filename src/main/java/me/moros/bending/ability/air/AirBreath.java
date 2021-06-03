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

package me.moros.bending.ability.air;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.FireTick;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AirBreath extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<AirStream> streams = new ArrayList<>();

  public AirBreath(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, AirBreath.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();

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
    user.entity().setRemainingAir(user.entity().getRemainingAir() - 5);
    Vector3 offset = new Vector3(0, -0.1, 0);
    Ray ray = new Ray(user.eyeLocation().add(offset), user.direction().multiply(userConfig.range));
    streams.add(new AirStream(ray));
    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).collect(Collectors.toList());
  }

  private class AirStream extends ParticleStream {
    private double distanceTravelled = 0;

    public AirStream(Ray ray) {
      super(user, ray, userConfig.speed, 0.5);
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
      livingOnly = false;
    }

    @Override
    public void render() {
      distanceTravelled += speed;
      Location spawnLoc = bukkitLocation();
      double offset = 0.15 * distanceTravelled;
      collider = new Sphere(location, collisionRadius + offset);
      if (MaterialUtil.isWater(spawnLoc.getBlock())) {
        ParticleUtil.create(Particle.WATER_BUBBLE, spawnLoc).count(3).offset(offset, offset, offset).spawn();
      } else {
        ParticleUtil.createAir(spawnLoc).count(NumberConversions.ceil(distanceTravelled)).offset(offset, offset, offset).spawn();
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.AIR.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      entity.setVelocity(ray.direction.normalize().multiply(userConfig.knockback).clampVelocity());
      FireTick.extinguish(entity);
      if (entity instanceof LivingEntity) {
        LivingEntity livingEntity = (LivingEntity) entity;
        livingEntity.setRemainingAir(livingEntity.getRemainingAir() + 1);
      }
      return false;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (BlockMethods.tryExtinguishFire(user, block)) {
        return false;
      }
      BlockMethods.tryCoolLava(user, block);
      if (!MaterialUtil.isTransparentOrWater(block) && user.pitch() > 30) {
        user.entity().setVelocity(user.direction().multiply(-userConfig.knockback).clampVelocity());
        FireTick.extinguish(user.entity());
      }
      return !MaterialUtil.isWater(block);
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.DURATION)
    public long duration;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airbreath");

      cooldown = abilityNode.node("cooldown").getLong(5000);
      range = abilityNode.node("range").getDouble(7.0);
      duration = abilityNode.node("duration").getLong(1000);
      speed = abilityNode.node("speed").getDouble(1.0);
      knockback = abilityNode.node("knockback").getDouble(0.5);
    }
  }
}
