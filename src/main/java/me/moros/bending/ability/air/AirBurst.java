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

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.BurstUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class AirBurst extends AbilityInstance {
  private enum Mode {CONE, SPHERE, FALL}

  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<AirStream> streams = new ArrayList<>();

  private boolean released;
  private long startTime;

  public AirBurst(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.ATTACK) {
      Bending.game().abilityManager(user.world()).firstInstance(user, AirBurst.class)
        .ifPresent(b -> b.release(Mode.CONE));
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (!released) {
      boolean charged = isCharged();
      if (charged) {
        ParticleUtil.air(user.mainHandSide().toLocation(user.world())).spawn();
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
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
      case CONE -> BurstUtil.cone(user, userConfig.coneRange);
      case FALL -> BurstUtil.fall(user, userConfig.sphereRange);
      default -> BurstUtil.sphere(user, userConfig.sphereRange);
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
        ParticleUtil.air(bukkitLocation()).offset(0.2, 0.2, 0.2).spawn();
        nextRenderTime = time + 75;
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.AIR.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      double factor = userConfig.power;
      BendingEffect.FIRE_TICK.reset(entity);
      if (factor == 0) {
        return false;
      }

      Vector3d push = ray.direction.normalize();
      // Cap vertical push
      push = push.setY(Math.max(-0.3, Math.min(0.3, push.getY())));

      factor *= 1 - (location.distance(ray.origin) / (2 * maxRange));
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
      EntityMethods.applyVelocity(AirBurst.this, entity, velocity);
      entity.setFallDistance(0);
      return false;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (BlockMethods.tryExtinguishFire(user, block)) {
        return false;
      }
      BlockMethods.tryCoolLava(user, block);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.CHARGE_TIME)
    public int chargeTime;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.STRENGTH)
    public double power;
    @Modifiable(Attribute.RANGE)
    public double sphereRange;
    @Modifiable(Attribute.RANGE)
    public double coneRange;
    public double fallThreshold;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airburst");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      chargeTime = abilityNode.node("charge-time").getInt(3500);
      speed = abilityNode.node("speed").getDouble(1.2);
      power = abilityNode.node("knockback").getDouble(1.2);
      coneRange = abilityNode.node("cone-range").getDouble(16.0);
      sphereRange = abilityNode.node("sphere-range").getDouble(12.0);
      fallThreshold = abilityNode.node("fall-threshold").getDouble(14.0);
    }
  }
}
