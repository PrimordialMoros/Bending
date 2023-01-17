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

package me.moros.bending.ability.fire;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.Element;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.Explosive;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.basic.ParticleStream;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.util.BendingExplosion;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Combustion extends AbilityInstance implements Explosive {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private CombustBeam beam;
  private Collider ignoreCollider;

  private boolean exploded;

  public Combustion(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.worldKey()).userInstances(user, Combustion.class).forEach(Combustion::explode);
      return false;
    }
    if (user.onCooldown(description())) {
      return false;
    }
    if (Policies.UNDER_WATER.test(user, description()) || Policies.UNDER_LAVA.test(user, description())) {
      return false;
    }
    this.user = user;
    loadConfig();
    beam = new CombustBeam();
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
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
    return beam.update();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return List.of(beam.collider());
  }

  @Override
  public void onCollision(Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (collidedAbility instanceof FireShield fireShield) {
      if (fireShield.isSphere()) {
        ignoreCollider = collision.colliderOther();
      }
      explode();
    } else if (collidedAbility instanceof Combustion other) {
      Vector3d first = collision.colliderSelf().position();
      Vector3d second = collision.colliderOther().position();
      Vector3d center = first.add(second).multiply(0.5);
      createExplosion(center, userConfig.power + other.userConfig.power, userConfig.damage + other.userConfig.damage);
      other.exploded = true;
    } else if (collidedAbility instanceof Explosive) {
      explode();
    } else if (collidedAbility.description().element() == Element.EARTH && collision.removeSelf()) {
      explode();
    }
  }

  @Override
  public void explode() {
    createExplosion(beam.location(), userConfig.power, userConfig.damage);
  }

  private void createExplosion(Vector3d center, double size, double damage) {
    if (exploded) {
      return;
    }
    exploded = true;
    Particle.FLAME.builder(center).extra(0.2).count(20).offset(1).spawn(user.world());
    Particle.LARGE_SMOKE.builder(center).extra(0.2).count(20).offset(1).spawn(user.world());
    Particle.FIREWORK.builder(center).extra(0.2).count(20).offset(1).spawn(user.world());

    Ray ray = new Ray(center, user.direction());
    FragileStructure.tryDamageStructure(user.world().nearbyBlocks(center, size), 0, ray);

    BendingExplosion.builder()
      .size(size)
      .damage(damage)
      .fireTicks(userConfig.fireTicks)
      .breakBlocks(true)
      .placeFire(true)
      .ignoreInsideCollider(ignoreCollider)
      .sound(6, 0.8F)
      .buildAndExplode(this, center);
  }

  private class CombustBeam extends ParticleStream {
    private static final SoundEffect LOUD_COMBUSTION = SoundEffect.COMBUSTION.with(2, 0);
    private double randomBeamDistance = 7;

    public CombustBeam() {
      super(user, user.ray(userConfig.range), 0.2, 1);
      canCollide = BlockType::isLiquid;
      singleCollision = true;
      steps = 6;
    }

    @Override
    public void render() {
      renderRing();
      Particle.SMOKE.builder(location).spawn(user.world());
      Particle.WAX_OFF.builder(location).extra(0.005).spawn(user.world());
    }

    @Override
    protected Vector3d controlDirection() {
      return user.direction().multiply(speed);
    }

    private void renderRing() {
      if (distanceTravelled >= randomBeamDistance) {
        LOUD_COMBUSTION.play(user.world(), location);
        randomBeamDistance = distanceTravelled + 4 + 2 * ThreadLocalRandom.current().nextGaussian();
        double radius = ThreadLocalRandom.current().nextDouble(3, 6);
        VectorUtil.circle(Vector3d.ONE, user.direction(), 20).forEach(v ->
          Particle.WAX_OFF.builder(location.add(v.multiply(0.2)))
            .count(0).offset(v.multiply(radius)).extra(0.9).spawn(user.world())
        );
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundEffect.COMBUSTION.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      explode();
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      explode();
      return true;
    }

    private Vector3d location() {
      return location;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 12_000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 4;
    @Modifiable(Attribute.FIRE_TICKS)
    private int fireTicks = 50;
    @Modifiable(Attribute.STRENGTH)
    private double power = 3.4;
    @Modifiable(Attribute.RANGE)
    private double range = 48;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "fire", "combustion");
    }
  }
}
