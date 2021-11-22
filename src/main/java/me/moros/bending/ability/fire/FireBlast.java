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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.AbilityInitializer;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.SoundEffect;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class FireBlast extends AbilityInstance implements Explosive {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private FireStream stream;
  private Collider ignoreCollider;

  private boolean charging;
  private boolean exploded = false;
  private double factor = 1.0;
  private long startTime;

  public FireBlast(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    startTime = System.currentTimeMillis();
    charging = true;

    if (user.mainHandSide().toBlock(user.world()).isLiquid()) {
      return false;
    }

    removalPolicy = Policies.builder().add(Policies.IN_LIQUID).build();

    for (FireBlast blast : Bending.game().abilityManager(user.world()).userInstances(user, FireBlast.class).toList()) {
      if (blast.charging) {
        blast.launch();
        return false;
      }
    }
    if (method == Activation.ATTACK) {
      launch();
    }
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (exploded || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (charging) {
      if (!description().equals(user.selectedAbility())) {
        return UpdateResult.REMOVE;
      }
      if (user.sneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
        ParticleUtil.fire(user, user.mainHandSide().toLocation(user.world())).spawn();
      } else if (!user.sneaking()) {
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
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
    Vector3d origin = user.mainHandSide();
    Vector3d lookingDir = user.direction().multiply(userConfig.range * factor);
    stream = new FireStream(new Ray(origin, lookingDir));
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return stream == null ? List.of() : List.of(stream.collider());
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    boolean fullyCharged = factor == userConfig.chargeFactor;
    if (fullyCharged && collision.removeSelf()) {
      String name = collidedAbility.description().name();
      if (AbilityInitializer.layer2.contains(name)) {
        collision.removeOther(true);
      } else {
        collision.removeSelf(false);
      }
    }
    if (fullyCharged && collidedAbility instanceof FireShield fireShield) {
      collision.removeOther(true);
      if (fireShield.isSphere()) {
        ignoreCollider = collision.colliderOther();
        explode();
      }
    } else if (collidedAbility instanceof FireBlast other) {
      double collidedFactor = other.factor;
      if (fullyCharged && collidedFactor == other.userConfig.chargeFactor) {
        Vector3d first = collision.colliderSelf().position();
        Vector3d second = collision.colliderOther().position();
        Vector3d center = first.add(second).multiply(0.5);
        double radius = userConfig.explosionRadius + other.userConfig.explosionRadius;
        double dmg = userConfig.damage + other.userConfig.damage;
        createExplosion(center, radius, dmg * (factor + other.factor - 1));
        other.exploded = true;
      } else if (factor > collidedFactor + 0.1) {
        collision.removeSelf(false);
      }
    } else if (fullyCharged && collidedAbility instanceof Explosive) {
      explode();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public void explode() {
    createExplosion(stream.location(), userConfig.explosionRadius, userConfig.damage * factor);
  }

  private void createExplosion(Vector3d center, double size, double damage) {
    if (exploded || factor < userConfig.chargeFactor) {
      return;
    }
    exploded = true;
    BendingExplosion.builder()
      .size(size)
      .damage(damage)
      .fireTicks(userConfig.fireTicks)
      .ignoreInsideCollider(ignoreCollider)
      .soundEffect(new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 5, 1))
      .buildAndExplode(this, center);
  }

  private class FireStream extends ParticleStream {
    private final double offset;
    private final double particleSpeed;
    private final int amount;
    private final boolean explosive;

    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed * factor, factor);
      canCollide = Block::isLiquid;
      offset = 0.25 + (factor - 1);
      particleSpeed = 0.02 * factor;
      amount = FastMath.ceil(6 * Math.pow(factor, 4));
      explosive = factor == userConfig.chargeFactor;
      singleCollision = explosive;
    }

    @Override
    public void render() {
      Location loc = bukkitLocation();
      ParticleUtil.fire(user, loc)
        .count(amount).offset(offset, offset, offset).extra(particleSpeed).spawn();
    }

    @Override
    public void postRender() {
      if (explosive || ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.FIRE.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (explosive) {
        explode();
        return true;
      }
      DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
      BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
      EntityMethods.applyVelocity(FireBlast.this, entity, ray.direction.normalize().multiply(0.5));
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      Vector3d reverse = ray.direction.negate();
      Location center = bukkitLocation();
      BlockMethods.tryLightBlock(block);
      if (user.location().distanceSq(Vector3d.center(block)) > 4) {
        for (Block b : WorldMethods.nearbyBlocks(center, userConfig.igniteRadius * factor)) {
          if (!user.canBuild(b)) {
            continue;
          }
          if (RayTrace.of(Vector3d.center(b), reverse).range(userConfig.igniteRadius + 2).result(user.world()).hit()) {
            continue;
          }
          if (MaterialUtil.isIgnitable(b)) {
            long delay = BendingProperties.FIRE_REVERT_TIME + ThreadLocalRandom.current().nextInt(1000);
            TempBlock.create(b, Material.FIRE.createBlockData(), delay, true);
          }
        }
      }
      FragileStructure.tryDamageStructure(List.of(block), FastMath.round(4 * factor));
      explode();
      return true;
    }

    private @NonNull Vector3d location() {
      return location;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.FIRE_TICKS)
    public int fireTicks;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.RADIUS)
    public double igniteRadius;
    @Modifiable(Attribute.RADIUS)
    public double explosionRadius;
    @Modifiable(Attribute.STRENGTH)
    public double chargeFactor;
    @Modifiable(Attribute.CHARGE_TIME)
    public long maxChargeTime;
    @Modifiable(Attribute.COOLDOWN)
    public long chargedCooldown;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "fireblast");

      cooldown = abilityNode.node("cooldown").getLong(1500);
      damage = abilityNode.node("damage").getDouble(2.0);
      fireTicks = abilityNode.node("fire-ticks").getInt(25);
      range = abilityNode.node("range").getDouble(18.0);
      speed = abilityNode.node("speed").getDouble(0.8);
      igniteRadius = abilityNode.node("ignite-radius").getDouble(1.5);
      explosionRadius = abilityNode.node("explosion-radius").getDouble(2.0);

      chargeFactor = Math.max(1, abilityNode.node("charge").node("factor").getDouble(1.5));
      maxChargeTime = abilityNode.node("charge").node("max-time").getLong(1500);
      chargedCooldown = abilityNode.node("charge").node("cooldown").getLong(500);

      abilityNode.node("charge").node("factor").comment("How much the damage, radius, range and speed are multiplied by at full charge");
      abilityNode.node("charge").node("max-time").comment("How many milliseconds it takes to fully charge");
    }
  }
}
