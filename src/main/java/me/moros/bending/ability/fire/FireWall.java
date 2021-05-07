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

package me.moros.bending.ability.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.OBB;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ExpiringSet;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FireWall extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Collection<Vector3> bases;
  private final Set<Entity> cachedEntities = new ExpiringSet<>(500);
  private final Set<Entity> damagedEntities = new ExpiringSet<>(500);
  private OBB collider;
  private Vector3 center;
  private Vector3 direction;

  private double currentHeight;
  private double height;
  private double distanceTravelled = 0;
  private long lastSneakTime;
  private long nextRenderTime;

  public FireWall(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();

    double hw = userConfig.width / 2.0;

    center = getValidBase(userConfig.height / 2.0);
    if (center == null) {
      return false;
    }

    direction = user.direction().setY(0).normalize();
    bases = setupBases();
    if (bases.isEmpty()) {
      return false;
    }

    height = userConfig.height;
    currentHeight = 1;

    AABB aabb = new AABB(new Vector3(-hw, -0.5, -0.6), new Vector3(hw, userConfig.maxHeight, 0.6));
    collider = new OBB(aabb, Vector3.PLUS_J, Math.toRadians(user.yaw())).at(center);

    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(userConfig.duration)).build();

    nextRenderTime = 0;
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
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

    if (time >= nextRenderTime) {
      nextRenderTime = time + 200;
      renderWall();
    }
    CollisionUtil.handleEntityCollisions(user, collider, this::onEntityHit, false, true);
    return UpdateResult.CONTINUE;
  }

  private void renderWall() {
    for (Vector3 base : bases) {
      for (double h = 0; h <= currentHeight; h += 0.8) {
        Vector3 pos = base.add(new Vector3(0, h, 0));
        Block block = pos.toBlock(user.world());
        double speed = 1 - (h / (2 * currentHeight));
        if (MaterialUtil.isTransparent(block)) {
          if (h == 0) {
            ParticleUtil.createFire(user, pos.toLocation(user.world())).count(10)
              .offset(0.5, 0.25, 0.5).extra(0.01).spawn();
          } else {
            for (int i = 0; i < 2; i++) {
              Vector3 center = VectorMethods.gaussianOffset(pos, 0.4);
              ParticleUtil.createFire(user, center.toLocation(user.world())).count(0)
                .offset(0, 1, 0).extra(0.07 * speed).spawn();
            }
          }
          if (ThreadLocalRandom.current().nextInt(15) == 0) {
            SoundUtil.FIRE_SOUND.play(pos.toLocation(user.world()));
          }
        }
      }
    }
  }

  private Vector3 getValidBase(double searchHeight) {
    Vector3 center = user.rayTrace(userConfig.range, false);
    for (double i = 0; i <= searchHeight; i += 0.5) {
      Vector3 check = center.subtract(new Vector3(0, i, 0));
      Block block = check.toBlock(user.world());
      if (!Bending.game().protectionSystem().canBuild(user, block)) {
        continue;
      }
      if (MaterialUtil.isTransparent(block) && block.getRelative(BlockFace.DOWN).isSolid()) {
        return check;
      }
    }
    return null;
  }

  private Collection<Vector3> setupBases() {
    double hw = userConfig.width / 2.0;
    Vector3 side = direction.crossProduct(Vector3.PLUS_J).normalize();
    Collection<Vector3> possibleBases = new ArrayList<>();
    for (double i = -hw; i < hw; i += 0.9) {
      Vector3 check = center.add(side.multiply(i));
      Block block = check.toBlock(user.world());
      if (MaterialUtil.isTransparent(block) && Bending.game().protectionSystem().canBuild(user, block)) {
        double baseY = NumberConversions.floor(check.y) + 0.25;
        possibleBases.add(check.setY(baseY));
      }
    }
    return possibleBases;
  }

  private void move() {
    if (distanceTravelled >= userConfig.maxRange) {
      return;
    }

    Vector3 currentPosition = center.add(direction);
    Block check = currentPosition.toBlock(user.world());
    if (!MaterialUtil.isTransparent(check) || !Bending.game().protectionSystem().canBuild(user, check)) {
      return;
    }
    center = currentPosition;
    collider = collider.at(center);
    bases = setupBases();
    distanceTravelled += direction.getNorm();

    if (currentHeight < userConfig.maxHeight) {
      double deltaHeight = (userConfig.maxHeight - userConfig.height) / userConfig.maxRange;
      currentHeight += deltaHeight;
    }
  }

  private boolean onEntityHit(Entity entity) {
    double requiredY = center.y + currentHeight;
    if (entity.getLocation().getY() > requiredY) {
      return false;
    }

    if (entity instanceof Projectile) {
      entity.remove();
      return true;
    }

    if (!(entity instanceof LivingEntity)) {
      entity.setVelocity(Vector3.ZERO.toBukkitVector());
      return true;
    }

    if (!cachedEntities.contains(entity)) {
      if (Bending.game().benderRegistry().user((LivingEntity) entity).map(HeatControl::canBurn).orElse(true)) {
        FireTick.ignite(user, entity);
        if (!damagedEntities.contains(entity)) {
          damagedEntities.add(entity);
          DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        }
        Vector3 pos = EntityMethods.entityCenter(entity);
        Vector3 velocity = pos.subtract(collider.closestPosition(pos)).normalize().multiply(userConfig.knockback);
        entity.setVelocity(velocity.clampVelocity());
        return true;
      } else {
        cachedEntities.add(entity);
      }
    }
    return false;
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return Collections.singletonList(collider);
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.HEIGHT)
    public double height;
    @Attribute(Attribute.RADIUS)
    public double width;
    @Attribute(Attribute.RANGE)
    public double range;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.STRENGTH)
    public double knockback;
    @Attribute(Attribute.DURATION)
    public long duration;

    @Attribute(Attribute.RANGE)
    public double maxRange;
    @Attribute(Attribute.HEIGHT)
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
