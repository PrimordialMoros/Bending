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
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.NonNull;

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
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();

    if (user.headBlock().isLiquid()) {
      return false;
    }

    removalPolicy = Policies.builder().build();

    user.addCooldown(description(), userConfig.cooldown);
    Vector3 origin = user.mainHandSide();
    Vector3 lookingDir = user.direction().scalarMultiply(userConfig.range);

    double length = user.velocity().subtract(user.direction()).getNorm();
    double factor = (length == 0) ? 1 : FastMath.max(0.5, FastMath.min(1.5, 1 / length));
    stream = new AirStream(new Ray(origin, lookingDir), 1.2, factor);
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
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
    return Collections.singletonList(stream.collider());
  }

  @Override
  public @NonNull User user() {
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
      VectorMethods.circle(Vector3.ONE.scalarMultiply(0.75), user.direction(), 10).forEach(v ->
        ParticleUtil.create(Particle.CLOUD, bukkitLocation().add(v.toVector()))
          .count(0).offset(v.getX(), v.getY(), v.getZ()).extra(-0.04).spawn()
      );
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.AIR_SOUND.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
      Vector3 velocity = EntityMethods.entityCenter(entity).subtract(ray.origin).normalize().scalarMultiply(factor);
      entity.setVelocity(velocity.clampVelocity());
      return true;
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
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.SPEED)
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
