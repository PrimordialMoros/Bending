/*
 * Copyright 2020-2026 Moros
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
import java.util.function.Consumer;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.ability.water.FrostBreath;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;

public class AirShield extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Vector3d center;

  private long currentPoint = 0;
  private long startTime;

  public AirShield(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(Policies.NOT_SNEAKING)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();
    startTime = System.currentTimeMillis();
    center = user.center();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBuild()) {
      return UpdateResult.REMOVE;
    }
    currentPoint++;
    center = user.center();
    double spacing = userConfig.radius / 4;
    for (int i = 1; i < 8; i++) {
      double y = (i * spacing) - userConfig.radius;
      double factor = 1 - (y * y) / (userConfig.radius * userConfig.radius);
      if (factor <= 0.2) {
        continue;
      }
      double x = userConfig.radius * factor * Math.cos(i * currentPoint);
      double z = userConfig.radius * factor * Math.sin(i * currentPoint);
      Vector3d loc = center.add(x, y, z);
      ParticleBuilder.air(loc).count(5).offset(0.2).spawn(user.world());
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundEffect.AIR.play(user.world(), loc);
      }
    }

    for (Block b : user.world().nearbyBlocks(center, userConfig.radius, MaterialUtil::isFire)) {
      WorldUtil.tryCoolLava(user, b);
      WorldUtil.tryExtinguishFire(user, b);
    }
    CollisionUtil.handle(user, Sphere.of(center, userConfig.radius), this::onEntityHit, false);
    return UpdateResult.CONTINUE;
  }

  private boolean onEntityHit(Entity entity) {
    Vector3d toEntity = entity.location().subtract(center);
    Vector3d normal = toEntity.withY(0).normalize();
    double strength = ((userConfig.radius - toEntity.length()) / userConfig.radius) * userConfig.maxPush;
    strength = Math.clamp(strength, 0, 1);
    entity.applyVelocity(this, entity.velocity().add(normal.multiply(strength)));
    return false;
  }

  @Override
  public void onDestroy() {
    double factor = userConfig.duration == 0 ? 1 : System.currentTimeMillis() - startTime / (double) userConfig.duration;
    long cooldown = Math.min(1000, (long) (factor * userConfig.cooldown));
    user.addCooldown(description(), cooldown);
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(Sphere.of(center, userConfig.radius));
  }

  private void forEachSurfaceBlock(Consumer<Block> consumer) {
    int r = FastMath.ceil(userConfig.radius);
    double rMinSq = (userConfig.radius - 1) * (userConfig.radius - 1);
    double rMaxSq = userConfig.radius * userConfig.radius;
    for (double x = center.x() - r; x <= center.x() + r; x++) {
      for (double y = center.y() - r; y <= center.y() + r; y++) {
        for (double z = center.z() - r; z <= center.z() + r; z++) {
          Vector3d loc = Vector3d.of(x, y, z);
          double distSq = center.distanceSq(loc);
          if (distSq < rMinSq || distSq > rMaxSq) {
            continue;
          }
          consumer.accept(user.world().blockAt(loc));
        }
      }
    }
  }

  @Override
  public void onCollision(Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (collidedAbility instanceof FrostBreath) {
      forEachSurfaceBlock(block -> {
        if (MaterialUtil.isTransparentOrWater(block) && user.canBuild(block)) {
          WorldUtil.tryBreakPlant(block);
          if (MaterialUtil.isAir(block) || MaterialUtil.isWater(block)) {
            TempBlock.ice().duration(BendingProperties.instance().iceRevertTime(1500)).build(block);
          }
        }
      });
    } else if (collidedAbility.description().elements().contains(Element.FIRE)) {
      double r = userConfig.radius;
      Vector3d pos = center.add(collision.colliderOther().position().subtract(center).normalize().multiply(r));
      ParticleBuilder.fire(collidedAbility.user(), pos).count(20).offset(0.1).extra(0.05).spawn(user.world());
      SoundEffect.FIRE_EXTINGUISH.play(user.world(), pos);
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 4000;
    @Modifiable(Attribute.DURATION)
    private long duration = 10_000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 4;
    @Modifiable(Attribute.STRENGTH)
    private double maxPush = 2.6;

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "airshield");
    }
  }
}
