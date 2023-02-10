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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.RayUtil;
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
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class FireBurst extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<FireStream> streams = MultiUpdatable.empty();
  private final Set<Entity> affectedEntities = new HashSet<>();

  private boolean released;
  private long startTime;

  public FireBurst(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.worldKey()).firstInstance(user, FireBurst.class)
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
        ParticleBuilder.fire(user, user.mainHandSide()).spawn(user.world());
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
    return streams.update();
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
      rays = RayUtil.cone(user, userConfig.coneRange);
    } else {
      rays = RayUtil.sphere(user, userConfig.sphereRange);
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
      canCollide = BlockType::isLiquid;
    }

    @Override
    public void render() {
      long time = System.currentTimeMillis();
      if (time >= nextRenderTime) {
        ParticleBuilder.fire(user, location).offset(0.2).extra(0.01).spawn(user.world());
        nextRenderTime = time + 75;
      }
      TempLight.builder(++ticks).build(user.world().blockAt(location));
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundEffect.FIRE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        entity.damage(userConfig.damage, user, description());
        BendingEffect.FIRE_TICK.apply(user, entity, userConfig.fireTicks);
        entity.applyVelocity(FireBurst.this, ray.direction.normalize().multiply(0.5));
      }
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      Vector3d reverse = ray.direction.negate();
      WorldUtil.tryLightBlock(block);
      double igniteRadius = 1.5;
      Vector3d standing = user.location().add(0, 0.5, 0);
      for (Block b : user.world().nearbyBlocks(location, igniteRadius)) {
        if (standing.distanceSq(b.center()) < 4 || !user.canBuild(b)) {
          continue;
        }
        if (user.rayTrace(b.center(), reverse).range(igniteRadius + 2).blocks(user.world()).hit()) {
          continue;
        }
        if (MaterialUtil.isIgnitable(b)) {
          TempBlock.fire().duration(BendingProperties.instance().fireRevertTime(1000))
            .ability(FireBurst.this).build(b);
        }
      }
      FragileStructure.tryDamageStructure(block, 4, new Ray(location, ray.direction));
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
    public List<String> path() {
      return List.of("abilities", "fire", "fireburst");
    }
  }
}
