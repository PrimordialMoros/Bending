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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.Explosive;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempLight;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.BendingExplosion;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.ability.AbilityInitializer;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class FireBlast extends AbilityInstance implements Explosive {
  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private FireStream stream;
  private Collider ignoreCollider;

  private boolean charging;
  private boolean exploded = false;
  private double factor = 1.0;
  private long startTime;

  public FireBlast(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    startTime = System.currentTimeMillis();
    charging = true;

    if (user.world().blockAt(user.mainHandSide()).type().isLiquid()) {
      return false;
    }

    removalPolicy = Policies.builder().add(Policies.UNDER_WATER).add(Policies.UNDER_LAVA).build();

    for (FireBlast blast : user.game().abilityManager(user.worldKey()).userInstances(user, FireBlast.class).toList()) {
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (exploded || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (charging) {
      if (!description().equals(user.selectedAbility())) {
        return UpdateResult.REMOVE;
      }
      if (user.sneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
        ParticleBuilder.fire(user, user.mainHandSide()).spawn(user.world());
      } else if (!user.sneaking()) {
        launch();
      }
      return UpdateResult.CONTINUE;
    }
    return stream.update();
  }

  private void launch() {
    long deltaTime = System.currentTimeMillis() - startTime;
    factor = 1;
    long cooldown = userConfig.cooldown;
    if (deltaTime >= userConfig.maxChargeTime) {
      factor = userConfig.chargeFactor;
      cooldown = userConfig.chargedCooldown;
    } else if (deltaTime > 0.3 * userConfig.maxChargeTime) {
      double deltaFactor = (userConfig.chargeFactor - factor) * deltaTime / userConfig.maxChargeTime;
      factor += deltaFactor;
    }
    charging = false;
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), cooldown);
    Vector3d origin = user.mainHandSide();
    Vector3d lookingDir = user.direction().multiply(userConfig.range * factor);
    stream = new FireStream(Ray.of(origin, lookingDir));
  }

  @Override
  public Collection<Collider> colliders() {
    return stream == null ? List.of() : List.of(stream.collider());
  }

  @Override
  public void onCollision(Collision collision) {
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
      .sound(5, 1)
      .buildAndExplode(this, center);
  }

  private class FireStream extends ParticleStream {
    private final double offset;
    private final double particleSpeed;
    private final int amount;
    private final boolean explosive;

    private int ticks = 3;

    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed * factor, 0.8 + 0.5 * (factor - 1));
      canCollide = BlockType::isLiquid;
      offset = 0.25 + (factor - 1);
      particleSpeed = 0.02 * factor;
      amount = FastMath.ceil(6 * Math.pow(factor, 4));
      explosive = factor == userConfig.chargeFactor;
      singleCollision = explosive;
    }

    @Override
    public void render() {
      ParticleBuilder.fire(user, location).count(amount).offset(offset).extra(particleSpeed).spawn(user.world());
      TempLight.builder(++ticks).build(user.world().blockAt(location));
    }

    @Override
    public void postRender() {
      if (explosive || ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (explosive) {
        explode();
        return true;
      }
      entity.damage(userConfig.damage * factor, user, description());
      BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
      entity.applyVelocity(FireBlast.this, ray.direction().normalize().multiply(0.5));
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      Vector3d reverse = ray.direction().negate();
      WorldUtil.tryLightBlock(block);
      Vector3d standing = user.location().add(0, 0.5, 0);
      for (Block b : user.world().nearbyBlocks(location, userConfig.igniteRadius * factor)) {
        if (standing.distanceSq(b.center()) < 4 || !user.canBuild(b)) {
          continue;
        }
        if (user.rayTrace(b.center(), reverse).range(userConfig.igniteRadius + 2).blocks(user.world()).hit()) {
          continue;
        }
        if (MaterialUtil.isIgnitable(b)) {
          TempBlock.fire().duration(BendingProperties.instance().fireRevertTime(1000))
            .ability(FireBlast.this).build(b);
        }
      }
      FragileStructure.tryDamageStructure(block, FastMath.round(4 * factor), Ray.of(location, ray.direction()));
      explode();
      return true;
    }

    private Vector3d location() {
      return location;
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 1500;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;
    @Modifiable(Attribute.FIRE_TICKS)
    private int fireTicks = 25;
    @Modifiable(Attribute.RANGE)
    private double range = 18;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.8;
    @Modifiable(Attribute.RADIUS)
    private double igniteRadius = 1.5;
    @Modifiable(Attribute.RADIUS)
    private double explosionRadius = 2.5;
    @Comment("How much the damage, radius, range and speed are multiplied by at full charge")
    @Modifiable(Attribute.STRENGTH)
    private double chargeFactor = 1.5;
    @Comment("How many milliseconds it takes to fully charge")
    @Modifiable(Attribute.CHARGE_TIME)
    private long maxChargeTime = 1500;
    @Modifiable(Attribute.COOLDOWN)
    private long chargedCooldown = 500;

    @Override
    public List<String> path() {
      return List.of("abilities", "fire", "fireblast");
    }
  }
}
