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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempLight;
import me.moros.bending.model.ExpiringSet;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class FireWall extends AbilityInstance {
  private static final Config config = new Config();

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

  public FireWall(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();

    double hw = userConfig.width / 2.0;

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

    AABB aabb = new AABB(new Vector3d(-hw, -0.5, -0.6), new Vector3d(hw, userConfig.maxHeight, 0.6));
    collider = new OBB(aabb, Vector3d.PLUS_J, Math.toRadians(user.yaw())).at(center);
    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(userConfig.duration)).build();
    nextRenderTime = 0;
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
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
        Vector3d pos = base.add(new Vector3d(0, h, 0));
        Block block = pos.toBlock(user.world());
        TempLight.builder(ticks).rate(1).duration(userConfig.duration).build(block)
          .map(TempLight::lock).ifPresent(l -> lights.put(block, l));
        double speed = 1 - (h / (2 * currentHeight));
        if (MaterialUtil.isTransparent(block)) {
          if (h == 0) {
            ParticleUtil.fire(user, pos).count(6).offset(0.5, 0.25, 0.5)
              .extra(0.01).spawn(user.world());
          } else {
            for (int i = 0; i < 2; i++) {
              Vector3d center = VectorUtil.gaussianOffset(pos, 0.4);
              ParticleUtil.fire(user, center).count(0).offset(0, 1, 0)
                .extra(0.07 * speed).spawn(user.world());
            }
          }
          if (ThreadLocalRandom.current().nextInt(15) == 0) {
            SoundUtil.FIRE.play(user.world(), pos);
          }
        }
      }
    }
    cleanupLight(oldLights);
  }

  private Vector3d getValidBase(double searchHeight) {
    Vector3d center = RayTrace.of(user).range(userConfig.range).ignoreLiquids(false).result(user.world()).position();
    for (double i = 0; i <= searchHeight; i += 0.5) {
      Vector3d check = center.subtract(new Vector3d(0, i, 0));
      Block block = check.toBlock(user.world());
      if (!user.canBuild(block)) {
        continue;
      }
      if (MaterialUtil.isTransparent(block) && block.getRelative(BlockFace.DOWN).isSolid()) {
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
      Block block = check.toBlock(user.world());
      if (MaterialUtil.isTransparent(block) && user.canBuild(block)) {
        double baseY = FastMath.floor(check.y()) + 0.25;
        possibleBases.add(check.withY(baseY));
      }
    }
    return possibleBases;
  }

  private void move() {
    if (distanceTravelled >= userConfig.maxRange) {
      return;
    }

    Vector3d currentPosition = center.add(direction);
    Block check = currentPosition.toBlock(user.world());
    if (!MaterialUtil.isTransparent(check) || !user.canBuild(check)) {
      return;
    }
    center = currentPosition;
    collider = collider.at(center);
    bases = setupBases();
    distanceTravelled += direction.length();

    if (currentHeight < userConfig.maxHeight) {
      double deltaHeight = (userConfig.maxHeight - userConfig.height) / userConfig.maxRange;
      currentHeight += deltaHeight;
    }
  }

  private boolean onEntityHit(Entity entity) {
    double requiredY = center.y() + currentHeight;
    if (entity.getLocation().getY() > requiredY) {
      return false;
    }

    if (entity instanceof Projectile) {
      entity.remove();
      return true;
    }

    if (!(entity instanceof LivingEntity)) {
      EntityUtil.applyVelocity(this, entity, Vector3d.ZERO);
      return true;
    }

    if (!cachedEntities.contains(entity)) {
      User entityUser = Registries.BENDERS.user((LivingEntity) entity);
      if (entityUser == null || HeatControl.canBurn(entityUser)) {
        BendingEffect.FIRE_TICK.apply(user, entity);
        if (!damagedEntities.contains(entity)) {
          damagedEntities.add(entity);
          DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        }
        Vector3d pos = EntityUtil.entityCenter(entity);
        Vector3d velocity = pos.subtract(collider.closestPosition(pos)).normalize().multiply(userConfig.knockback);
        EntityUtil.applyVelocity(this, entity, velocity);
        return true;
      } else {
        cachedEntities.add(entity);
      }
    }
    return false;
  }

  private void cleanupLight(Collection<TempLight> collection) {
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(collider);
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.HEIGHT)
    public double height;
    @Modifiable(Attribute.RADIUS)
    public double width;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;
    @Modifiable(Attribute.DURATION)
    public long duration;

    @Modifiable(Attribute.RANGE)
    public double maxRange;
    @Modifiable(Attribute.HEIGHT)
    public double maxHeight;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "firewall");

      cooldown = abilityNode.node("cooldown").getLong(20000);
      height = abilityNode.node("height").getDouble(4);
      width = abilityNode.node("width").getDouble(6.0);
      range = abilityNode.node("range").getDouble(3.0);
      damage = abilityNode.node("damage").getDouble(0.5);
      knockback = abilityNode.node("knockback").getDouble(0.33);
      duration = abilityNode.node("duration").getLong(8000);

      maxRange = abilityNode.node("move-range").getDouble(7.0);
      maxHeight = abilityNode.node("move-max-height").getDouble(8.0);
    }
  }
}
