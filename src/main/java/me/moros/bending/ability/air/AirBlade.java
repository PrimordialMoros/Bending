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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.air.sequences.AirWheel;
import me.moros.bending.ability.common.basic.AbstractWheel;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AirBlade extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Vector3d origin;
  private Vector3d direction;
  private Blade blade;

  private boolean charging;
  private double factor = 1;
  private long startTime;

  public AirBlade(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, AirBlade.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    charging = true;
    direction = user.direction().setY(0).normalize();
    double maxRadius = userConfig.radius * userConfig.chargeFactor * 0.5;
    origin = user.location().add(direction).add(new Vector3d(0, maxRadius, 0));

    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(OutOfRangeRemovalPolicy.of(userConfig.prepareRange, () -> origin))
      .add(Policies.IN_LIQUID)
      .build();

    startTime = System.currentTimeMillis();

    AirWheel wheel = Bending.game().abilityManager(user.world()).firstInstance(user, AirWheel.class).orElse(null);
    if (wheel != null) {
      origin = wheel.center();
      factor = userConfig.chargeFactor;
      charging = false;
      blade = new Blade(new Ray(origin, direction), userConfig.speed * factor * 0.5);
      removalPolicy = Policies.builder()
        .add(OutOfRangeRemovalPolicy.of(userConfig.range * factor, origin, () -> blade.location())).build();
      user.addCooldown(description(), userConfig.cooldown);
      Bending.game().abilityManager(user.world()).destroyInstance(wheel);
    }
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
      if (origin.toBlock(user.world()).isLiquid()) {
        return UpdateResult.REMOVE;
      }
      long time = System.currentTimeMillis();
      if (user.sneaking() && time > startTime + 100) {
        double timeFactor = Math.min(0.9, (time - startTime) / (double) userConfig.maxChargeTime);
        Vector3d rotateAxis = Vector3d.PLUS_J.cross(direction);
        double r = userConfig.radius * userConfig.chargeFactor * timeFactor * 0.5;
        VectorMethods.circle(direction.multiply(r), rotateAxis, 20).forEach(v ->
          ParticleUtil.createAir(origin.add(v).toLocation(user.world())).spawn()
        );
      } else if (!user.sneaking()) {
        launch();
      }
      return UpdateResult.CONTINUE;
    }

    return blade.update();
  }

  private void launch() {
    long deltaTime = System.currentTimeMillis() - startTime;
    factor = 1;
    if (deltaTime >= userConfig.maxChargeTime) {
      factor = userConfig.chargeFactor;
    } else if (deltaTime > 0.3 * userConfig.maxChargeTime) {
      double deltaFactor = (userConfig.chargeFactor - factor) * deltaTime / (double) userConfig.maxChargeTime;
      factor += deltaFactor;
    }
    charging = false;
    blade = new Blade(new Ray(origin, direction));
    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.range * factor, origin, () -> blade.location())).build();
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return blade == null ? List.of() : List.of(blade.collider());
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (collidedAbility instanceof AirBlade) {
      double collidedFactor = ((AirBlade) collidedAbility).factor;
      if (factor - collidedFactor > 0.1) {
        collision.removeSelf(false);
      }
    }
  }

  private class Blade extends AbstractWheel {
    public Blade(Ray ray) {
      super(user, ray, userConfig.radius * factor * 0.5, userConfig.speed * factor * 0.5);
    }

    // When started from wheel
    public Blade(Ray ray, double speed) {
      super(user, ray, 1.6, speed);
    }

    @Override
    public void render() {
      Vector3d rotateAxis = Vector3d.PLUS_J.cross(this.ray.direction);
      VectorMethods.circle(this.ray.direction.multiply(this.radius), rotateAxis, 40).forEach(v ->
        ParticleUtil.createAir(location.add(v).toLocation(user.world())).spawn()
      );
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.AIR.play(location.toLocation(user.world()));
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
      Vector3d velocity = direction.setY(userConfig.knockup).normalize().multiply(userConfig.knockback);
      EntityMethods.applyVelocity(AirBlade.this, entity, velocity);
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      BlockMethods.tryExtinguishFire(user, block);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RADIUS)
    public double radius;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;
    @Modifiable(Attribute.STRENGTH)
    public double knockup;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.RANGE)
    public double prepareRange;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.CHARGE_TIME)
    public long maxChargeTime;
    @Modifiable(Attribute.STRENGTH)
    public double chargeFactor;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airblade");

      cooldown = abilityNode.node("cooldown").getLong(4000);
      radius = abilityNode.node("radius").getDouble(1.2);
      damage = abilityNode.node("damage").getDouble(1.5);
      knockback = abilityNode.node("knockback").getDouble(0.8);
      knockup = abilityNode.node("knockup").getDouble(0.15);
      range = abilityNode.node("range").getDouble(12.0);
      prepareRange = abilityNode.node("prepare-range").getDouble(8.0);
      speed = abilityNode.node("speed").getDouble(0.8);

      chargeFactor = abilityNode.node("charge").node("factor").getDouble(3.0);
      maxChargeTime = abilityNode.node("charge").node("max-time").getLong(2000);

      abilityNode.node("speed").comment("How many blocks the blade advances every tick.");
      abilityNode.node("charge").node("factor").comment("How much the damage and range are multiplied by at full charge. Radius and speed are only affected by half that amount.");
      abilityNode.node("charge").node("max-time").comment("How many milliseconds it takes to fully charge");

    }
  }
}

