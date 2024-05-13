/*
 * Copyright 2020-2024 Moros
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
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirPunch extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private AirStream stream;

  public AirPunch(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    Vector3d origin = user.mainHandSide();
    Vector3d lookingDir = user.direction().multiply(userConfig.range);

    if (user.world().blockAt(origin).type().isLiquid()) {
      return false;
    }

    user.addCooldown(description(), userConfig.cooldown);
    double length = user.velocity().subtract(user.direction()).length();
    double factor = (length == 0) ? 1 : FastMath.clamp(1 / length, 0.5, 1.5);
    Sound.ENTITY_BREEZE_SHOOT.asEffect(1, (float) factor).play(user.world(), origin);
    stream = new AirStream(Ray.of(origin, lookingDir), 1.2, factor);
    removalPolicy = Policies.defaults();
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
    return stream.update();
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(stream.collider());
  }

  private class AirStream extends ParticleStream {
    private final double factor;

    public AirStream(Ray ray, double collisionRadius, double factor) {
      super(user, ray, userConfig.speed * factor, collisionRadius);
      this.factor = factor;
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
    }

    @Override
    public void render() {
      VectorUtil.circle(Vector3d.ONE.multiply(0.75), user.direction(), 10).forEach(v ->
        Particle.CLOUD.builder(location.add(v)).count(0).offset(v).extra(-0.04).spawn(user.world())
      );
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.AIR.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      entity.damage(userConfig.damage * factor, user, description());
      Vector3d velocity = entity.center().subtract(ray.position()).normalize().multiply(factor);
      entity.applyVelocity(AirPunch.this, velocity);
      return true;
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
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 2500;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3;
    @Modifiable(Attribute.RANGE)
    private double range = 18;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.8;

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "airpunch");
    }
  }
}
