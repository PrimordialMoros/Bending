/*
 * Copyright 2020-2024 Moros
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
import me.moros.bending.api.ability.element.Element;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingExplosion;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Combustion extends AbilityInstance implements Explosive {
  private static final Config config = ConfigManager.load(Config::new);

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
    removalPolicy = Policies.defaults();
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
    } else if (collidedAbility.description().elements().contains(Element.EARTH) && collision.removeSelf()) {
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

    Ray ray = Ray.of(center, user.direction());
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

    private Vector3d streamDirection;
    private double randomBeamDistance = 7;

    public CombustBeam() {
      super(user, user.ray(userConfig.range), 0.2, 1);
      canCollide = BlockType::isLiquid;
      singleCollision = true;
      steps = 6;
      streamDirection = dir;
    }

    @Override
    public void render() {
      renderRing();
      Particle.SMOKE.builder(location).spawn(user.world());
      Particle.WAX_OFF.builder(location).extra(0.005).spawn(user.world());
    }

    @Override
    protected Vector3d controlDirection() {
      if (description().equals(user.selectedAbility())) {
        streamDirection = user.direction().multiply(speed);
      }
      return streamDirection;
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
  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "fire", "combustion");
    }
  }
}
