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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.ability.common.basic.ParticleStream;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempLight;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class FireBurst extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<FireStream> streams = new ArrayList<>();
  private final Set<Entity> affectedEntities = new HashSet<>();

  private boolean released;
  private long startTime;

  public FireBurst(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.world()).firstInstance(user, FireBurst.class)
        .ifPresent(b -> b.release(true));
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    released = false;
    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (!released) {
      boolean charged = isCharged();
      if (charged) {
        ParticleUtil.fire(user, user.mainHandSide()).spawn(user.world());
        if (!user.sneaking()) {
          release(false);
        }
      } else {
        if (!user.sneaking()) {
          return UpdateResult.REMOVE;
        }
      }
      return UpdateResult.CONTINUE;
    }

    streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    return streams.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  @Override
  public void onCollision(Collision collision) {
    Collider collider = collision.colliderSelf();
    streams.removeIf(stream -> stream.collider().equals(collider));
    if (collision.removeSelf() && !streams.isEmpty()) {
      collision.removeSelf(false);
    }
  }

  private boolean isCharged() {
    return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
  }

  private void release(boolean cone) {
    if (released || !isCharged()) {
      return;
    }
    released = true;
    Collection<Ray> rays;
    if (cone) {
      rays = VectorUtil.cone(user, userConfig.coneRange);
    } else {
      rays = VectorUtil.sphere(user, userConfig.sphereRange);
    }
    rays.forEach(r -> streams.add(new FireStream(r)));
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
  }

  private class FireStream extends ParticleStream {
    private long nextRenderTime;
    private int ticks = 3;

    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed, 1);
      canCollide = Block::isLiquid;
    }

    @Override
    public void render() {
      long time = System.currentTimeMillis();
      if (time >= nextRenderTime) {
        ParticleUtil.fire(user, location).offset(0.2).extra(0.01).spawn(user.world());
        nextRenderTime = time + 75;
      }
      TempLight.builder(++ticks).build(location.toBlock(user.world()));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
        EntityUtil.applyVelocity(FireBurst.this, entity, ray.direction.normalize().multiply(0.5));
      }
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      Vector3d reverse = ray.direction.negate();
      WorldUtil.tryLightBlock(block);
      double igniteRadius = 1.5;
      Vector3d standing = user.location().add(0, 0.5, 0);
      for (Block b : WorldUtil.nearbyBlocks(user.world(), location, igniteRadius)) {
        if (standing.distanceSq(Vector3d.center(b)) < 4 || !user.canBuild(b)) {
          continue;
        }
        if (user.rayTrace(Vector3d.center(b), reverse).range(igniteRadius + 2).blocks(user.world()).hit()) {
          continue;
        }
        if (MaterialUtil.isIgnitable(b)) {
          TempBlock.fire().duration(BendingProperties.instance().fireRevertTime(1000)).build(b);
        }
      }
      FragileStructure.tryDamageStructure(List.of(block), 4);
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.CHARGE_TIME)
    private long chargeTime = 2500;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3;
    @Modifiable(Attribute.FIRE_TICKS)
    private int fireTicks = 35;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.8;
    @Modifiable(Attribute.RANGE)
    private double sphereRange = 7;
    @Modifiable(Attribute.RANGE)
    private double coneRange = 11;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "fire", "fireburst");
    }
  }
}
