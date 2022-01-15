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

import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.AbilityInitializer;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Rotation;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class FlameRush extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private FireStream stream;
  private final Set<Entity> affectedEntities = new HashSet<>();

  private boolean charging;
  private boolean fullyCharged = false;
  private long startTime;

  public FlameRush(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, FlameRush.class)) {
      return false;
    }

    this.user = user;
    loadConfig();
    startTime = System.currentTimeMillis();
    charging = true;

    if (Policies.IN_LIQUID.test(user, description())) {
      return false;
    }

    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(Policies.IN_LIQUID)
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

    if (charging) {
      if (user.sneaking()) {
        ParticleUtil.fire(user, user.mainHandSide().toLocation(user.world())).spawn();
        if (System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
          ParticleUtil.of(Particle.SMOKE_NORMAL, user.mainHandSide().toLocation(user.world())).spawn();
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
    Vector3d origin = user.location().add(new Vector3d(0, 1.2, 0));
    Vector3d lookingDir = user.direction().multiply(userConfig.range * factor);
    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    stream = new FireStream(new Ray(origin, lookingDir), factor);
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return stream == null ? List.of() : List.of(stream.collider());
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
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
    private final double factor;

    private Vector3d streamDirection;
    private double currentPoint = 0;
    private double distanceTravelled = 0;

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
      distanceTravelled += speed;
      double radius = 0.2 * factor + 0.6 * (distanceTravelled / maxRange);
      int amount = FastMath.ceil(12 * radius);
      double offset = 0.5 * radius;
      ParticleUtil.fire(user, bukkitLocation()).count(amount).offset(offset, offset, offset).spawn();
      Vector3d vec = new Rotation(streamDirection, currentPoint).applyTo(Vector3d.ONE.multiply(radius));
      Location spiral1 = location.add(vec).toLocation(user.world());
      Location spiral2 = location.subtract(vec).toLocation(user.world());
      ParticleUtil.fire(user, spiral1).spawn();
      ParticleUtil.fire(user, spiral2).spawn();
      ParticleUtil.of(Particle.SMOKE_LARGE, spiral1).spawn();
      ParticleUtil.of(Particle.SMOKE_LARGE, spiral2).spawn();
      collider = new Sphere(location, collisionRadius + 0.7 * radius);
    }

    public @NonNull Vector3d controlDirection() {
      streamDirection = streamDirection.add(user.direction().multiply(0.08)).normalize().multiply(speed);
      return streamDirection;
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.FIRE.play(bukkitLocation(), 2, 1);
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
        BendingEffect.FIRE_TICK.apply(user, entity);
        EntityUtil.applyVelocity(FlameRush.this, entity, streamDirection.normalize().multiply(0.9));
      }
      return false;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      FragileStructure.tryDamageStructure(List.of(block), FastMath.round(8 * factor));
      return true;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.STRENGTH)
    public double chargeFactor;
    @Modifiable(Attribute.CHARGE_TIME)
    public long maxChargeTime;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "flamerush");

      cooldown = abilityNode.node("cooldown").getLong(10000);
      damage = abilityNode.node("damage").getDouble(2.0);
      range = abilityNode.node("range").getDouble(16.0);
      speed = abilityNode.node("speed").getDouble(1.2);

      chargeFactor = Math.max(1, abilityNode.node("charge").node("factor").getDouble(2.0));
      maxChargeTime = abilityNode.node("charge").node("max-time").getLong(2500);

      abilityNode.node("charge").node("factor").comment("How much the damage and range are multiplied by at full charge");
      abilityNode.node("charge").node("max-time").comment("How many milliseconds it takes to fully charge");
    }
  }
}
