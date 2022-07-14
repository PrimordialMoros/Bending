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
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirBreath extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<AirStream> streams = new ArrayList<>();

  public AirBreath(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.world()).hasAbility(user, AirBreath.class)) {
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
    user.entity().setRemainingAir(Math.max(-20, user.entity().getRemainingAir() - 5));
    Vector3d offset = new Vector3d(0, -0.1, 0);
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
      collider = new Sphere(location, collisionRadius + offset);
      Block block = location.toBlock(user.world());
      if (MaterialUtil.isWater(block)) {
        ParticleUtil.bubble(block).spawn(user.world());
      } else {
        ParticleUtil.air(location).count(FastMath.ceil(distanceTravelled)).offset(offset).spawn(user.world());
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.AIR.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      EntityUtil.applyVelocity(AirBreath.this, entity, ray.direction.normalize().multiply(userConfig.knockback));
      BendingEffect.FIRE_TICK.reset(entity);
      if (entity instanceof LivingEntity livingEntity) {
        livingEntity.setRemainingAir(Math.min(livingEntity.getMaximumAir(), livingEntity.getRemainingAir() + 1));
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
        EntityUtil.applyVelocity(AirBreath.this, user.entity(), user.direction().multiply(-userConfig.knockback));
        BendingEffect.FIRE_TICK.reset(user.entity());
      }
      return !MaterialUtil.isWater(block);
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
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
    public Iterable<String> path() {
      return List.of("abilities", "air", "airbreath");
    }
  }
}
