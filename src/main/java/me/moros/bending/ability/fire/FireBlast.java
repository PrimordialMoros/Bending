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

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.AbilityInitializer;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Burstable;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.FireTick;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.NumberConversions;

public class FireBlast extends AbilityInstance implements Ability, Explosive, Burstable {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private FireStream stream;
  private Collider ignoreCollider;

  private boolean charging;
  private boolean hasExploded = false;
  private double factor = 1.0;
  private int particleCount = 6;
  private long renderInterval = 0;
  private long startTime;

  public FireBlast(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();
    startTime = System.currentTimeMillis();
    charging = true;

    if (user.getHeadBlock().isLiquid()) {
      return false;
    }

    removalPolicy = Policies.builder().build();

    for (FireBlast blast : Bending.getGame().getAbilityManager(user.getWorld()).getUserInstances(user, FireBlast.class).collect(Collectors.toList())) {
      if (blast.charging) {
        blast.launch();
        return false;
      }
    }
    if (method == ActivationMethod.ATTACK) {
      launch();
    }
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (hasExploded || removalPolicy.test(user, getDescription())) {
      return UpdateResult.REMOVE;
    }
    if (charging) {
      if (!getDescription().equals(user.getSelectedAbility().orElse(null))) {
        return UpdateResult.REMOVE;
      }
      if (user.isSneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
        ParticleUtil.createFire(user, user.getMainHandSide().toLocation(user.getWorld())).spawn();
      } else if (!user.isSneaking()) {
        launch();
      }
    }
    return (charging || stream.update() == UpdateResult.CONTINUE) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
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
    user.setCooldown(getDescription(), userConfig.cooldown);
    Vector3 origin = user.getMainHandSide();
    Vector3 lookingDir = user.getDirection().scalarMultiply(userConfig.range * factor);
    stream = new FireStream(new Ray(origin, lookingDir), 1 * factor);
  }

  @Override
  public @NonNull Collection<@NonNull Collider> getColliders() {
    if (stream == null) {
      return Collections.emptyList();
    }
    return Collections.singletonList(stream.getCollider());
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
    Ability collidedAbility = collision.getCollidedAbility();
    boolean fullyCharged = factor == userConfig.chargeFactor;
    if (fullyCharged && collision.shouldRemoveSelf()) {
      String name = collidedAbility.getDescription().getName();
      if (AbilityInitializer.layer2.contains(name)) {
        collision.setRemoveCollided(true);
      } else {
        collision.setRemoveSelf(false);
      }
    }
    if (fullyCharged && collidedAbility instanceof FireShield) {
      collision.setRemoveCollided(true);
      boolean sphere = ((FireShield) collidedAbility).isSphere();
      if (sphere) {
        ignoreCollider = collision.getColliders().getValue();
      }
      explode();
    } else if (collidedAbility instanceof FireBlast) {
      FireBlast other = (FireBlast) collidedAbility;
      double collidedFactor = other.factor;
      if (fullyCharged && collidedFactor == other.userConfig.chargeFactor) {
        Vector3 first = collision.getColliders().getKey().getPosition();
        Vector3 second = collision.getColliders().getValue().getPosition();
        Vector3 center = first.add(second).scalarMultiply(0.5);
        double radius = userConfig.explosionRadius + other.userConfig.explosionRadius;
        double dmg = userConfig.damage + other.userConfig.damage;
        createExplosion(center, radius, dmg * (factor + other.factor - 1));
        other.hasExploded = true;
      } else if (factor > collidedFactor + 0.1) {
        collision.setRemoveSelf(false);
      }
    } else if (fullyCharged && collidedAbility instanceof Explosive) {
      explode();
    }
  }

  @Override
  public @NonNull User getUser() {
    return user;
  }

  // Used to initialize the blast for bursts
  @Override
  public void initialize(@NonNull User user, @NonNull Vector3 location, @NonNull Vector3 direction) {
    this.user = user;
    recalculateConfig();
    factor = 1.0;
    charging = false;
    removalPolicy = Policies.builder().build();
    particleCount = 1;
    renderInterval = 75;
    stream = new FireStream(new Ray(location, direction), 1);
  }

  @Override
  public void explode() {
    createExplosion(stream.getLocation(), userConfig.explosionRadius, userConfig.damage * factor);
  }

  private void createExplosion(Vector3 center, double size, double damage) {
    if (hasExploded || factor < userConfig.chargeFactor) {
      return;
    }
    hasExploded = true;
    Location loc = center.toLocation(user.getWorld());
    ParticleUtil.create(Particle.EXPLOSION_HUGE, loc).spawn();
    SoundUtil.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 5, 1);

    double halfSize = size / 2;
    Sphere collider = new Sphere(center, size);
    CollisionUtil.handleEntityCollisions(user, collider, entity -> {
      Vector3 entityCenter = EntityMethods.getEntityCenter(entity);
      double distance = center.distance(entityCenter);
      double distanceFactor = (distance <= halfSize) ? 1 : 1 - ((distance - halfSize) / size);
      if (ignoreCollider == null || !ignoreCollider.contains(entityCenter)) {
        DamageUtil.damageEntity(entity, user, damage * distanceFactor, getDescription());
        FireTick.LARGER.apply(user, entity, userConfig.fireTick);
      }
      double knockback = factor * distanceFactor * BendingProperties.EXPLOSION_KNOCKBACK;
      if (entity.equals(user.getEntity())) {
        knockback *= 0.5;
      }
      Vector3 dir = entityCenter.subtract(center).normalize().scalarMultiply(knockback);
      entity.setVelocity(dir.clampVelocity());
      return true;
    }, true, true);
  }

  private class FireStream extends ParticleStream {
    private double offset = 0.25;
    private double particleSpeed = 0.03;
    private final int amount;
    private final boolean explosive;
    private long nextRenderTime;

    public FireStream(Ray ray, double collisionRadius) {
      super(user, ray, userConfig.speed * factor, collisionRadius);
      canCollide = Block::isLiquid;
      if (factor > 1) {
        offset += factor - 1;
        particleSpeed *= factor;
        amount = NumberConversions.ceil(particleCount * 3 * factor);
      } else {
        amount = particleCount;
      }
      explosive = factor == userConfig.chargeFactor;
      singleCollision = explosive;
    }

    @Override
    public void render() {
      long time = System.currentTimeMillis();
      if (renderInterval == 0 || time >= nextRenderTime) {
        Location loc = getBukkitLocation();
        ParticleUtil.createFire(user, loc)
          .count(amount).offset(offset, offset, offset).extra(particleSpeed).spawn();
        nextRenderTime = time + renderInterval;
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.FIRE_SOUND.play(getBukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (explosive) {
        explode();
        return true;
      }
      DamageUtil.damageEntity(entity, user, userConfig.damage * factor, getDescription());
      FireTick.LARGER.apply(user, entity, userConfig.fireTick);
      entity.setVelocity(ray.direction.normalize().scalarMultiply(0.5).clampVelocity());
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      Vector3 reverse = ray.direction.scalarMultiply(-1);
      Location center = getBukkitLocation();
      if (user.getLocation().distanceSq(new Vector3(block)) > 4) {
        for (Block b : WorldMethods.getNearbyBlocks(center, userConfig.igniteRadius * factor)) {
          if (!Bending.getGame().getProtectionSystem().canBuild(user, b)) {
            continue;
          }
          if (WorldMethods.blockCast(user.getWorld(), new Ray(new Vector3(b), reverse), userConfig.igniteRadius * factor + 2).isPresent()) {
            continue;
          }
          BlockMethods.tryLightBlock(b);
          if (MaterialUtil.isIgnitable(b)) {
            long delay = BendingProperties.FIRE_REVERT_TIME + ThreadLocalRandom.current().nextInt(1000);
            TempBlock.create(b, Material.FIRE.createBlockData(), delay, true);
          }
        }
      }
      FragileStructure.tryDamageStructure(Collections.singletonList(block), NumberConversions.round(4 * factor));
      explode();
      return true;
    }

    private @NonNull Vector3 getLocation() {
      return location;
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
    @Attribute(Attribute.DURATION)
    public int fireTick;
    @Attribute(Attribute.RADIUS)
    public double igniteRadius;
    @Attribute(Attribute.RADIUS)
    public double explosionRadius;
    @Attribute(Attribute.STRENGTH)
    public double chargeFactor;
    @Attribute(Attribute.CHARGE_TIME)
    public long maxChargeTime;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "fireblast");

      cooldown = abilityNode.node("cooldown").getLong(1500);
      damage = abilityNode.node("damage").getDouble(2.5);
      range = abilityNode.node("range").getDouble(18.0);
      speed = abilityNode.node("speed").getDouble(0.8);
      fireTick = abilityNode.node("fire-tick").getInt(20);
      igniteRadius = abilityNode.node("ignite-radius").getDouble(1.5);
      explosionRadius = abilityNode.node("explosion-radius").getDouble(2.0);

      chargeFactor = FastMath.max(1, abilityNode.node("charge").node("factor").getDouble(1.5));
      maxChargeTime = abilityNode.node("charge").node("max-time").getLong(1500);

      abilityNode.node("charge").node("factor").comment("How much the damage, radius, range and speed are multiplied by at full charge");
      abilityNode.node("charge").node("max-time").comment("How many milliseconds it takes to fully charge");
    }
  }
}
