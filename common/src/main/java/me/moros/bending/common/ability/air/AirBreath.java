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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirBreath extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<AirStream> streams = MultiUpdatable.empty();

  public AirBreath(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, AirBreath.class)) {
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    user.remainingAir(Math.max(-20, user.remainingAir() - 5));
    Vector3d offset = Vector3d.of(0, -0.1, 0);
    Ray ray = Ray.of(user.eyeLocation().add(offset), user.direction().multiply(userConfig.range));
    streams.add(new AirStream(ray));
    return streams.update();
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  private class AirStream extends ParticleStream {
    public AirStream(Ray ray) {
      super(user, ray, userConfig.speed, 0.5);
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
      livingOnly = false;
    }

    @Override
    public void render() {
      double offset = 0.15 * distanceTravelled;
      collider = Sphere.of(location, collisionRadius + offset);
      Block block = user.world().blockAt(location);
      if (MaterialUtil.isWater(block)) {
        ParticleBuilder.bubble(block).spawn(user.world());
      } else {
        ParticleBuilder.air(location).count(FastMath.ceil(distanceTravelled)).offset(offset).spawn(user.world());
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundEffect.AIR.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      entity.applyVelocity(AirBreath.this, ray.direction().normalize().multiply(userConfig.knockback));
      BendingEffect.FIRE_TICK.reset(entity);
      if (entity instanceof LivingEntity livingEntity) {
        livingEntity.remainingAir(Math.min(livingEntity.airCapacity(), livingEntity.remainingAir() + 1));
      }
      return false;
    }

    @Override
    public boolean onBlockHit(Block block) {
      if (WorldUtil.tryExtinguishFire(user, block)) {
        return false;
      }
      WorldUtil.tryCoolLava(user, block);
      if (!MaterialUtil.isTransparentOrWater(block) && user.pitch() > 30) {
        user.applyVelocity(AirBreath.this, user.direction().multiply(-userConfig.knockback));
        BendingEffect.FIRE_TICK.reset(user);
      }
      return !MaterialUtil.isWater(block);
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 5000;
    @Modifiable(Attribute.RANGE)
    private double range = 7;
    @Modifiable(Attribute.DURATION)
    private long duration = 1000;
    @Modifiable(Attribute.SPEED)
    private double speed = 1;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 0.5;

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "airbreath");
    }
  }
}
