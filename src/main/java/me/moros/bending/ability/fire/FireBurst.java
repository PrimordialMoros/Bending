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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.ActivationMethod;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.BurstUtil;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.FireTick;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FireBurst extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<FireStream> streams = new ArrayList<>();
  private final Set<Entity> affectedEntities = new HashSet<>();

  private boolean released;
  private long startTime;

  public FireBurst(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    if (method == ActivationMethod.ATTACK) {
      Bending.game().abilityManager(user.world()).firstInstance(user, FireBurst.class)
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
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (!released) {
      boolean charged = isCharged();
      if (charged) {
        ParticleUtil.createFire(user, user.mainHandSide().toLocation(user.world())).spawn();
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).collect(Collectors.toList());
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
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
      rays = BurstUtil.cone(user, userConfig.coneRange);
    } else {
      rays = BurstUtil.sphere(user, userConfig.sphereRange);
    }
    rays.forEach(r -> streams.add(new FireStream(r)));
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
  }

  private class FireStream extends ParticleStream {
    private long nextRenderTime;

    public FireStream(Ray ray) {
      super(user, ray, userConfig.speed, 1);
      canCollide = Block::isLiquid;
    }

    @Override
    public void render() {
      long time = System.currentTimeMillis();
      if (time >= nextRenderTime) {
        Location loc = bukkitLocation();
        ParticleUtil.createFire(user, loc).offset(0.2, 0.2, 0.2).extra(0.01).spawn();
        nextRenderTime = time + 75;
      }
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.FIRE.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (!affectedEntities.contains(entity)) {
        affectedEntities.add(entity);
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        FireTick.ignite(user, entity, userConfig.fireTicks);
        entity.setVelocity(ray.direction.normalize().multiply(0.5).clampVelocity());
      }
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      Vector3 reverse = ray.direction.negate();
      Location center = bukkitLocation();
      BlockMethods.tryLightBlock(block);
      double igniteRadius = 1.5;
      if (user.location().distanceSq(Vector3.center(block)) > 4) {
        for (Block b : WorldMethods.nearbyBlocks(center, igniteRadius)) {
          if (!user.canBuild(b)) {
            continue;
          }
          if (WorldMethods.rayTraceBlocks(user.world(), new Ray(Vector3.center(b), reverse), igniteRadius + 2).isPresent()) {
            continue;
          }
          if (MaterialUtil.isIgnitable(b)) {
            long delay = BendingProperties.FIRE_REVERT_TIME + ThreadLocalRandom.current().nextInt(1000);
            TempBlock.create(b, Material.FIRE.createBlockData(), delay, true);
          }
        }
      }
      FragileStructure.tryDamageStructure(List.of(block), 4);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.CHARGE_TIME)
    public int chargeTime;
    @Attribute(Attribute.DAMAGE)
    public double damage;
    @Attribute(Attribute.FIRE_TICKS)
    public int fireTicks;
    @Attribute(Attribute.SPEED)
    public double speed;

    @Attribute(Attribute.RANGE)
    public double sphereRange;
    @Attribute(Attribute.RANGE)
    public double coneRange;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "fireburst");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      chargeTime = abilityNode.node("charge-time").getInt(3500);
      damage = abilityNode.node("damage").getDouble(3.0);
      fireTicks = abilityNode.node("fire-ticks").getInt(35);
      speed = abilityNode.node("speed").getDouble(0.8);
      coneRange = abilityNode.node("cone-range").getDouble(11.0);
      sphereRange = abilityNode.node("sphere-range").getDouble(7.0);

    }
  }
}
