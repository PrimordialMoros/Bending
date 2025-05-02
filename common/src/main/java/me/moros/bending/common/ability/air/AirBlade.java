/*
 * Copyright 2020-2025 Moros
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

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.AbstractWheel;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.OutOfRangeRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.common.ability.air.sequence.AirWheel;
import me.moros.math.FastMath;
import me.moros.math.Rotation;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class AirBlade extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Vector3d origin;
  private Vector3d direction;
  private Blade blade;

  private boolean charging;
  private double factor = 1;
  private long startTime;
  private double chargingPoint;

  public AirBlade(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, AirBlade.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    charging = true;
    direction = user.direction().withY(0).normalize();
    double maxRadius = userConfig.radius * userConfig.chargeFactor * 0.5;
    origin = user.location().add(direction).add(0, maxRadius, 0);

    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(OutOfRangeRemovalPolicy.of(userConfig.prepareRange, () -> origin))
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
      .build();

    startTime = System.currentTimeMillis();

    AirWheel wheel = user.game().abilityManager(user.worldKey()).firstInstance(user, AirWheel.class).orElse(null);
    if (wheel != null) {
      origin = wheel.center();
      factor = userConfig.chargeFactor;
      charging = false;
      blade = new Blade(Ray.of(origin, direction), userConfig.speed * factor * 0.5);
      removalPolicy = Policies.builder()
        .add(OutOfRangeRemovalPolicy.of(userConfig.range * factor, origin, () -> blade.location())).build();
      user.addCooldown(description(), userConfig.cooldown);
      user.game().abilityManager(user.worldKey()).destroyInstance(wheel);
    }
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (charging) {
      if (user.world().blockAt(origin).type().isLiquid()) {
        return UpdateResult.REMOVE;
      }
      long time = System.currentTimeMillis();
      if (user.sneaking() && time > startTime + 100) {
        double timeFactor = Math.min(0.9, (time - startTime) / (double) userConfig.maxChargeTime);
        double r = userConfig.radius * userConfig.chargeFactor * timeFactor * 0.5;
        int amount = FastMath.ceil(r * 10);
        Rotation rotation = createNextRotation(Vector3d.PLUS_J.cross(direction), amount);
        VectorUtil.rotate(direction.multiply(r), rotation, amount).forEach(v ->
          ParticleBuilder.air(origin.add(v)).spawn(user.world())
        );
        double[] offset = direction.toArray();
        for (double d = 0.1; d < r; d += 0.25) {
          rotation.applyTo(offset, offset);
          ParticleBuilder.air(origin.add(Vector3d.from(offset).multiply(d))).spawn(user.world());
        }
        if (ThreadLocalRandom.current().nextInt(8) == 0) {
          SoundEffect.AIR.play(user.world(), origin);
        }
      } else if (!user.sneaking()) {
        launch();
      }
      return UpdateResult.CONTINUE;
    }
    return blade.update();
  }

  private Rotation createNextRotation(Vector3d rotateAxis, int amount) {
    chargingPoint += Math.PI / 36;
    return Rotation.from(rotateAxis, chargingPoint).applyTo(Rotation.from(rotateAxis, 2 * Math.PI / amount));
  }

  private void launch() {
    long deltaTime = System.currentTimeMillis() - startTime;
    factor = 1;
    if (deltaTime >= userConfig.maxChargeTime) {
      factor = userConfig.chargeFactor;
    } else if (deltaTime > 0.3 * userConfig.maxChargeTime) {
      double deltaFactor = (userConfig.chargeFactor - factor) * deltaTime / userConfig.maxChargeTime;
      factor += deltaFactor;
    }
    charging = false;
    blade = new Blade(Ray.of(origin, direction));
    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.range * factor, origin, () -> blade.location())).build();
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public Collection<Collider> colliders() {
    return blade == null ? List.of() : List.of(blade.collider());
  }

  @Override
  public void onCollision(Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (collidedAbility instanceof AirBlade other) {
      if (factor - other.factor > 0.1) {
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
    public void render(Vector3d location) {
      int amount = FastMath.ceil(22 * radius);
      Rotation rotation = createNextRotation(Vector3d.PLUS_J.cross(this.ray.direction()), amount);
      VectorUtil.rotate(this.ray.direction().multiply(this.radius), rotation, amount).forEach(v ->
        ParticleBuilder.air(location.add(v)).spawn(user.world())
      );
    }

    @Override
    public void postRender(Vector3d location) {
      if (ThreadLocalRandom.current().nextInt(8) == 0) {
        SoundEffect.AIR.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      entity.damage(userConfig.damage * factor, user, description());
      Vector3d velocity = direction.withY(userConfig.knockup).normalize().multiply(userConfig.knockback);
      entity.applyVelocity(AirBlade.this, velocity);
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      WorldUtil.tryExtinguishFire(user, block);
      return true;
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 4000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 1.2;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 1.5;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 0.8;
    @Modifiable(Attribute.STRENGTH)
    private double knockup = 0.15;
    @Modifiable(Attribute.RANGE)
    private double range = 12;
    @Modifiable(Attribute.RANGE)
    private double prepareRange = 8;
    @Comment("How many blocks the blade advances every tick")
    @Modifiable(Attribute.SPEED)
    private double speed = 0.8;
    @Comment("How many milliseconds it takes to fully charge")
    @Modifiable(Attribute.CHARGE_TIME)
    private long maxChargeTime = 2000;
    @Comment("How much the damage and range are multiplied by at full charge. Radius and speed are only affected by half that amount")
    @Modifiable(Attribute.STRENGTH)
    private double chargeFactor = 3;

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "airblade");
    }
  }
}

