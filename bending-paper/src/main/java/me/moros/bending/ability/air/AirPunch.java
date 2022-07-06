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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class AirPunch extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private AirStream stream;

  public AirPunch(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();

    Vector3d origin = user.mainHandSide();
    Vector3d lookingDir = user.direction().multiply(userConfig.range);

    if (origin.toBlock(user.world()).isLiquid()) {
      return false;
    }

    user.addCooldown(description(), userConfig.cooldown);
    double length = user.velocity().subtract(user.direction()).length();
    double factor = (length == 0) ? 1 : Math.max(0.5, Math.min(1.5, 1 / length));
    stream = new AirStream(new Ray(origin, lookingDir), 1.2, factor);
    removalPolicy = Policies.builder().build();
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
    return stream.update();
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(stream.collider());
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
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
        ParticleUtil.of(Particle.CLOUD, location.add(v)).count(0).offset(v).extra(-0.04).spawn(user.world())
      );
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.AIR.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
      Vector3d velocity = EntityUtil.entityCenter(entity).subtract(ray.origin).normalize().multiply(factor);
      EntityUtil.applyVelocity(AirPunch.this, entity, velocity);
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (WorldUtil.tryExtinguishFire(user, block)) {
        return false;
      }
      WorldUtil.tryCoolLava(user, block);
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

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airpunch");

      cooldown = abilityNode.node("cooldown").getLong(2500);
      damage = abilityNode.node("damage").getDouble(3.0);
      range = abilityNode.node("range").getDouble(18.0);
      speed = abilityNode.node("speed").getDouble(0.8);
    }
  }
}
