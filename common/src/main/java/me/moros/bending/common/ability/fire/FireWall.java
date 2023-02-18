/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.common.ability.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.OBB;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.temporal.TempLight;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.ExpiringSet;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class FireWall extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Collection<Vector3d> bases;
  private final Map<Block, TempLight> lights = new HashMap<>();
  private Collection<TempLight> oldLights;
  private final ExpiringSet<Entity> cachedEntities = new ExpiringSet<>(500);
  private final ExpiringSet<Entity> damagedEntities = new ExpiringSet<>(500);
  private OBB collider;
  private Vector3d center;
  private Vector3d direction;

  private double currentHeight;
  private double height;
  private double distanceTravelled = 0;
  private long lastSneakTime;
  private long nextRenderTime;

  private int ticks = 5;

  public FireWall(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    center = getValidBase(userConfig.height / 2.0);
    if (center == null) {
      return false;
    }

    direction = user.direction().withY(0).normalize();
    bases = setupBases();
    if (bases.isEmpty()) {
      return false;
    }

    height = userConfig.height;
    currentHeight = 1;

    double hw = 0.5 + userConfig.width / 2.0;
    AABB aabb = AABB.of(Vector3d.of(-hw, -0.5, -0.75), Vector3d.of(hw, userConfig.moveMaxHeight, 0.75));
    collider = OBB.of(aabb, Vector3d.PLUS_J, Math.toRadians(user.yaw())).at(center);
    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(userConfig.duration)).build();
    nextRenderTime = 0;
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (bases.isEmpty() || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (currentHeight < height) {
      currentHeight += height / 60;
    }

    long time = System.currentTimeMillis();
    if (user.sneaking() && user.selectedAbilityName().equals("FireWall")) {
      if (lastSneakTime == 0) {
        lastSneakTime = time;
      }
      if (time >= lastSneakTime + 250) {
        lastSneakTime = time;
        move();
      }
    } else {
      lastSneakTime = 0;
    }
    ++ticks;
    if (time >= nextRenderTime) {
      nextRenderTime = time + 200;
      renderWall();
    }
    CollisionUtil.handle(user, collider, this::onEntityHit, false, true);
    return UpdateResult.CONTINUE;
  }

  private void renderWall() {
    for (Vector3d base : bases) {
      for (double h = 0; h <= currentHeight; h += 0.8) {
        Vector3d pos = base.add(0, h, 0);
        Block block = user.world().blockAt(pos);
        if (MaterialUtil.isTransparent(block)) {
          TempLight.builder(ticks).rate(1).duration(userConfig.duration).build(block)
            .map(TempLight::lock).ifPresent(l -> lights.put(block, l));
          if (h == 0) {
            ParticleBuilder.fire(user, pos).count(6).offset(0.5, 0.25, 0.5)
              .extra(0.01).spawn(user.world());
          } else {
            double speed = 1 - (h / (2 * currentHeight));
            for (int i = 0; i < 2; i++) {
              Vector3d center = VectorUtil.gaussianOffset(pos, 0.4);
              ParticleBuilder.fire(user, center).count(0).offset(0, 1, 0)
                .extra(0.07 * speed).spawn(user.world());
            }
          }
          if (ThreadLocalRandom.current().nextInt(15) == 0) {
            SoundEffect.FIRE.play(user.world(), pos);
          }
        }
      }
    }
    cleanupLight(oldLights);
  }

  private @Nullable Vector3d getValidBase(double searchHeight) {
    Vector3d center = user.rayTrace(userConfig.range).ignoreLiquids(false).blocks(user.world()).position();
    for (double i = 0; i <= searchHeight; i += 0.5) {
      Vector3d check = center.subtract(0, i, 0);
      Block block = user.world().blockAt(check);
      if (!user.canBuild(block)) {
        continue;
      }
      if (MaterialUtil.isTransparent(block) && block.offset(Direction.DOWN).type().isSolid()) {
        return check;
      }
    }
    return null;
  }

  private Collection<Vector3d> setupBases() {
    oldLights = new ArrayList<>(lights.values());
    lights.clear();
    double hw = userConfig.width / 2.0;
    Vector3d side = direction.cross(Vector3d.PLUS_J).normalize();
    Collection<Vector3d> possibleBases = new ArrayList<>();
    for (double i = -hw; i < hw; i += 0.9) {
      Vector3d check = center.add(side.multiply(i));
      double baseY = FastMath.floor(check.y()) + 0.25;
      possibleBases.add(check.withY(baseY));
    }
    return possibleBases;
  }

  private void move() {
    if (distanceTravelled >= userConfig.moveRange) {
      return;
    }

    Vector3d currentPosition = center.add(direction);
    Block check = user.world().blockAt(currentPosition);
    if (!MaterialUtil.isTransparent(check) || !user.canBuild(check)) {
      return;
    }
    center = currentPosition;
    collider = collider.at(center);
    bases = setupBases();
    distanceTravelled += direction.length();

    if (currentHeight < userConfig.moveMaxHeight) {
      double deltaHeight = (userConfig.moveMaxHeight - userConfig.height) / userConfig.moveRange;
      currentHeight += deltaHeight;
    }
  }

  private boolean onEntityHit(Entity entity) {
    double requiredY = center.y() + currentHeight;
    if (entity.location().y() > requiredY) {
      return false;
    }

    if (entity.isProjectile()) {
      entity.remove();
      return true;
    }

    if (!(entity instanceof LivingEntity)) {
      entity.applyVelocity(this, Vector3d.ZERO);
      return true;
    }

    if (!cachedEntities.contains(entity)) {
      User entityUser = Registries.BENDERS.get(entity.uuid());
      if (entityUser == null || HeatControl.canBurn(entityUser)) {
        BendingEffect.FIRE_TICK.apply(user, entity);
        if (!damagedEntities.contains(entity)) {
          damagedEntities.add(entity);
          entity.damage(userConfig.damage, user, description());
        }
        Vector3d pos = entity.center();
        double d = direction.dot(pos.subtract(center));
        Vector3d altVel = d > 0 ? direction : direction.negate();
        Vector3d velocity = pos.subtract(collider.closestPosition(pos)).normalize(altVel).multiply(userConfig.knockback);
        entity.applyVelocity(this, velocity);
        return true;
      } else {
        cachedEntities.add(entity);
      }
    }
    return false;
  }

  private void cleanupLight(@Nullable Collection<TempLight> collection) {
    if (collection != null && !collection.isEmpty()) {
      collection.forEach(TempLight::unlockAndRevert);
      collection.clear();
    }
  }

  @Override
  public void onDestroy() {
    cleanupLight(oldLights);
    cleanupLight(lights.values());
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(collider);
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 20_000;
    @Modifiable(Attribute.HEIGHT)
    private double height = 4;
    @Modifiable(Attribute.RADIUS)
    private double width = 6;
    @Modifiable(Attribute.RANGE)
    private double range = 3;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 0.5;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 0.33;
    @Modifiable(Attribute.DURATION)
    private long duration = 8000;
    @Modifiable(Attribute.RANGE)
    private double moveRange = 7;
    @Modifiable(Attribute.HEIGHT)
    private double moveMaxHeight = 8;

    @Override
    public List<String> path() {
      return List.of("abilities", "fire", "firewall");
    }
  }
}
