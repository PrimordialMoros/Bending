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

package me.moros.bending.ability.fire;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.AbilityInitializer;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.basic.ParticleStream;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Rotation;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempLight;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.SoundUtil.SoundEffect;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class FlameRush extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private FireStream stream;
  private final Set<Entity> affectedEntities = new HashSet<>();

  private boolean charging;
  private boolean fullyCharged = false;
  private long startTime;

  public FlameRush(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.world()).hasAbility(user, FlameRush.class)) {
      return false;
    }
    if (Policies.UNDER_WATER.test(user, description()) || Policies.UNDER_LAVA.test(user, description())) {
      return false;
    }
    this.user = user;
    loadConfig();
    startTime = System.currentTimeMillis();
    charging = true;

    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
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

    if (charging) {
      if (user.sneaking()) {
        Vector3d spawnLoc = user.mainHandSide();
        ParticleUtil.fire(user, spawnLoc).spawn(user.world());
        if (System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
          ParticleUtil.of(Particle.SMOKE_NORMAL, spawnLoc).spawn(user.world());
        }
      } else {
        launch();
      }
    }

    return (charging || stream.update() == UpdateResult.CONTINUE) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  public boolean isFullyCharged() {
    return fullyCharged;
  }

  private void launch() {
    long time = System.currentTimeMillis();
    double deltaTime = time - startTime;
    double factor = 1;
    if (deltaTime >= userConfig.maxChargeTime) {
      factor = userConfig.chargeFactor;
      fullyCharged = true;
    } else if (deltaTime > 0.3 * userConfig.maxChargeTime) {
      double deltaFactor = (userConfig.chargeFactor - factor) * deltaTime / userConfig.maxChargeTime;
      factor += deltaFactor;
    }
    charging = false;
    user.addCooldown(description(), userConfig.cooldown);
    Vector3d origin = user.location().add(0, 1.2, 0);
    Vector3d lookingDir = user.direction().multiply(userConfig.range * factor);
    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    stream = new FireStream(new Ray(origin, lookingDir), factor);
  }

  @Override
  public Collection<Collider> colliders() {
    return stream == null ? List.of() : List.of(stream.collider());
  }

  @Override
  public void onCollision(Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (fullyCharged && collision.removeSelf()) {
      String name = collidedAbility.description().name();
      if (AbilityInitializer.layer3.contains(name)) {
        collision.removeOther(true);
      } else {
        collision.removeSelf(false);
      }
    } else if (collidedAbility instanceof FlameRush other) {
      double collidedFactor = other.stream.factor;
      if (stream.factor > collidedFactor + 0.1) {
        collision.removeSelf(false);
      }
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class FireStream extends ParticleStream {
    private static final SoundEffect LOUD_FIRE = SoundUtil.FIRE.with(2, 1);

    private final double factor;

    private Vector3d streamDirection;
    private double currentPoint = 0;

    private int ticks = 0;

    public FireStream(Ray ray, double factor) {
      super(user, ray, userConfig.speed / 3, 0.5);
      this.factor = factor;
      canCollide = Block::isLiquid;
      steps = 3;
      streamDirection = dir;
    }

    @Override
    public void render() {
      currentPoint += Math.PI / 30;
      double radius = 0.2 * factor + 0.6 * (distanceTravelled / maxRange);
      int amount = FastMath.ceil(12 * radius);
      double offset = 0.5 * radius;
      ParticleUtil.fire(user, location).count(amount).offset(offset).spawn(user.world());
      Vector3d vec = new Rotation(streamDirection, currentPoint).applyTo(Vector3d.ONE.multiply(radius));
      Vector3d spiral1 = location.add(vec);
      Vector3d spiral2 = location.subtract(vec);
      ParticleUtil.fire(user, spiral1).spawn(user.world());
      ParticleUtil.fire(user, spiral2).spawn(user.world());
      ParticleUtil.of(Particle.SMOKE_LARGE, spiral1).spawn(user.world());
      ParticleUtil.of(Particle.SMOKE_LARGE, spiral2).spawn(user.world());
      collider = new Sphere(location, collisionRadius + 0.7 * radius);
      TempLight.builder(++ticks).build(location.toBlock(user.world()));
    }

    @Override
    protected Vector3d controlDirection() {
      streamDirection = streamDirection.add(user.direction().multiply(0.08)).normalize().multiply(speed);
      return streamDirection;
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        LOUD_FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
        BendingEffect.FIRE_TICK.apply(user, entity);
        EntityUtil.applyVelocity(FlameRush.this, entity, streamDirection.normalize().multiply(0.9));
      }
      return false;
    }

    @Override
    public boolean onBlockHit(Block block) {
      FragileStructure.tryDamageStructure(List.of(block), FastMath.round(8 * factor));
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 10_000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;
    @Modifiable(Attribute.RANGE)
    private double range = 16;
    @Modifiable(Attribute.SPEED)
    private double speed = 1.2;
    @Comment("How much the damage and range are multiplied by at full charge")
    @Modifiable(Attribute.STRENGTH)
    private double chargeFactor = 2;
    @Comment("How many milliseconds it takes to fully charge")
    @Modifiable(Attribute.CHARGE_TIME)
    private long maxChargeTime = 2500;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "fire", "flamerush");
    }
  }
}
