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

package me.moros.bending.ability.air;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.basic.ParticleStream;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirBurst extends AbilityInstance {
  private enum Mode {CONE, SPHERE, FALL}

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<AirStream> streams = new ArrayList<>();

  private boolean released;
  private long startTime;

  public AirBurst(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.world()).firstInstance(user, AirBurst.class).ifPresent(b -> b.release(Mode.CONE));
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    released = false;
    if (method == Activation.FALL) {
      if (user.entity().getFallDistance() < userConfig.fallThreshold || user.sneaking()) {
        return false;
      }
      release(Mode.FALL);
    }
    startTime = System.currentTimeMillis();
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

    if (!released) {
      boolean charged = isCharged();
      if (charged) {
        ParticleUtil.air(user.mainHandSide()).spawn(user.world());
        if (!user.sneaking()) {
          release(Mode.SPHERE);
        }
      } else {
        if (!user.sneaking()) {
          return UpdateResult.REMOVE;
        }
      }
      return UpdateResult.CONTINUE;
    }

    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  @Override
  public void onCollision(Collision collision) {
    Collider collider = collision.colliderSelf();
    streams.removeIf(stream -> stream.collider().equals(collider));
    if (collision.removeSelf() && !streams.isEmpty()) {
      collision.removeSelf(false);
    }
  }

  private boolean isCharged() {
    return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
  }

  private void release(Mode mode) {
    if (released || !isCharged()) {
      return;
    }
    released = true;
    Collection<Ray> rays = switch (mode) {
      case CONE -> VectorUtil.cone(user, userConfig.coneRange);
      case FALL -> VectorUtil.fall(user, userConfig.sphereRange);
      default -> VectorUtil.sphere(user, userConfig.sphereRange);
    };
    rays.forEach(r -> streams.add(new AirStream(r)));
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
  }

  private class AirStream extends ParticleStream {
    private long nextRenderTime;

    public AirStream(Ray ray) {
      super(user, ray, userConfig.speed, 1.3);
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
      livingOnly = false;
    }

    @Override
    public void render() {
      long time = System.currentTimeMillis();
      if (time >= nextRenderTime) {
        ParticleUtil.air(location).offset(0.2).spawn(user.world());
        nextRenderTime = time + 75;
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.AIR.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      double factor = userConfig.knockback;
      BendingEffect.FIRE_TICK.reset(entity);
      if (factor == 0) {
        return false;
      }

      Vector3d push = ray.direction.normalize();
      // Cap vertical push
      push = push.withY(Math.max(-0.3, Math.min(0.3, push.y())));

      factor *= 1 - (distanceTravelled / (2 * maxRange));
      Vector3d velocity = new Vector3d(entity.getVelocity());
      // The strength of the entity's velocity in the direction of the blast.
      double strength = velocity.dot(push.normalize());
      if (strength > factor) {
        double f = velocity.normalize().dot(push.normalize());
        velocity = velocity.multiply(0.5).add(push.normalize().multiply(f));
      } else if (strength + factor * 0.5 > factor) {
        velocity = velocity.add(push.multiply(factor - strength));
      } else {
        velocity = velocity.add(push.multiply(factor * 0.5));
      }
      EntityUtil.applyVelocity(AirBurst.this, entity, velocity);
      entity.setFallDistance(0);
      return false;
    }

    @Override
    public boolean onBlockHit(Block block) {
      if (WorldUtil.tryExtinguishFire(user, block)) {
        return false;
      }
      WorldUtil.tryCoolLava(user, block);
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.CHARGE_TIME)
    private long chargeTime = 2500;
    @Modifiable(Attribute.SPEED)
    private double speed = 1.2;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 1.2;
    @Modifiable(Attribute.RANGE)
    private double sphereRange = 12;
    @Modifiable(Attribute.RANGE)
    private double coneRange = 16;
    private double fallThreshold = 14;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "air", "airburst");
    }
  }
}
