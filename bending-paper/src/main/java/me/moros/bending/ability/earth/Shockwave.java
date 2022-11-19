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

package me.moros.bending.ability.earth;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ExpiringSet;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.MultiUpdatable;
import me.moros.bending.model.ability.common.basic.BlockLine;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.temporal.TempEntity.TempEntityType;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Shockwave extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);
  private static final Vector3d OFFSET = Vector3d.of(0.4, 0.75, 0.4);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<Ripple> streams = MultiUpdatable.empty();
  private final Set<Entity> affectedEntities = new HashSet<>();
  private final Set<Block> affectedBlocks = new HashSet<>();
  private final ExpiringSet<Block> recentAffectedBlocks = new ExpiringSet<>(500);
  private Collection<Collider> colliders = List.of();
  private Vector3d origin;

  private boolean released;
  private double range;
  private long startTime;

  public Shockwave(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.world()).firstInstance(user, Shockwave.class)
        .ifPresent(s -> s.release(true));
      return false;
    }

    if (user.game().abilityManager(user.world()).hasAbility(user, Shockwave.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    released = false;
    if (method == Activation.FALL) {
      if (user.entity().getFallDistance() < userConfig.fallThreshold || user.sneaking()) {
        return false;
      }
      if (!release(false)) {
        return false;
      }
    }

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
        ParticleUtil.of(Particle.SMOKE_NORMAL, user.mainHandSide()).spawn(user.world());
        if (!user.sneaking() && !release(false)) {
          return UpdateResult.REMOVE;
        }
      } else {
        if (!user.sneaking()) {
          return UpdateResult.REMOVE;
        }
      }
      return UpdateResult.CONTINUE;
    }

    colliders = recentAffectedBlocks.snapshot().stream()
      .map(b -> (Collider) AABB.BLOCK_BOUNDS.grow(OFFSET).at(Vector3d.of(b.getX(), b.getY() + 0.5, b.getZ())))
      .toList();
    if (!colliders.isEmpty()) {
      CollisionUtil.handle(user, new Sphere(origin, range + 2), this::onEntityHit, false);
    }
    return streams.update();
  }

  private boolean onEntityHit(Entity entity) {
    if (!affectedEntities.contains(entity)) {
      Vector3d loc = Vector3d.from(entity.getLocation());
      AABB entityCollider = AABBUtil.entityBounds(entity);
      for (Collider aabb : colliders) {
        if (aabb.intersects(entityCollider)) {
          DamageUtil.damageEntity(entity, user, userConfig.damage, description());
          double deltaY = Math.min(0.8, 0.4 + loc.distance(origin) / (1.5 * range));
          Vector3d push = loc.subtract(origin).normalize().withY(deltaY).multiply(userConfig.knockback);
          EntityUtil.applyVelocity(this, entity, push);
          affectedEntities.add(entity);
          return true;
        }
      }
    }
    return false;
  }

  private boolean isCharged() {
    return System.currentTimeMillis() >= startTime + userConfig.chargeTime;
  }

  private boolean release(boolean cone) {
    if (released || !isCharged() || !user.isOnGround()) {
      return false;
    }
    released = true;
    range = cone ? userConfig.coneRange : userConfig.ringRange;

    origin = user.location().blockCenter();
    Vector3d dir = user.direction().withY(0).normalize();
    if (cone) {
      double deltaAngle = Math.PI / (3 * range);
      VectorUtil.createArc(dir, Vector3d.PLUS_J, deltaAngle, FastMath.ceil(range / 1.5)).forEach(v ->
        streams.add(new Ripple(new Ray(origin, v.multiply(range)), 0))
      );
    } else {
      VectorUtil.circle(dir, Vector3d.PLUS_J, FastMath.ceil(6 * range)).forEach(v ->
        streams.add(new Ripple(new Ray(origin, v.multiply(range)), 75))
      );
    }

    // First update in same tick to only apply cooldown if there are valid ripples
    if (streams.update() == UpdateResult.REMOVE) {
      return false;
    }
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    return colliders;
  }

  private class Ripple extends BlockLine {
    public Ripple(Ray ray, long interval) {
      super(user, ray);
      this.interval = interval;
    }

    @Override
    public boolean isValidBlock(Block block) {
      if (block.isLiquid() || !MaterialUtil.isTransparent(block)) {
        return false;
      }
      return EarthMaterials.isEarthbendable(user, block.getRelative(BlockFace.DOWN));
    }

    @Override
    public void render(Block block) {
      if (!affectedBlocks.add(block)) {
        return;
      }
      recentAffectedBlocks.add(block);
      WorldUtil.tryBreakPlant(block);
      if (MaterialUtil.isFire(block)) {
        block.setType(Material.AIR);
      }
      double deltaY = Math.min(0.25, 0.05 + distanceTravelled / (3 * range));
      Vector3d velocity = Vector3d.of(0, deltaY, 0);
      Block below = block.getRelative(BlockFace.DOWN);
      BlockData data = mapData(below.getBlockData());
      TempEntity.builder(data).velocity(velocity).duration(500)
        .build(TempEntityType.FALLING_BLOCK, user.world(), Vector3d.fromCenter(below));
      ParticleUtil.of(Particle.BLOCK_CRACK, Vector3d.fromCenter(block).add(0, 0.75, 0))
        .count(5).offset(0.5, 0.25, 0.5).data(data).spawn(user.world());
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.EARTH.play(block);
      }
    }
  }

  // Use different block type due to https://bugs.mojang.com/browse/MC-114286
  private static BlockData mapData(BlockData data) {
    if (data.getMaterial() == Material.SAND) {
      return Material.SANDSTONE.createBlockData();
    } else if (data.getMaterial() == Material.RED_SAND) {
      return Material.RED_SANDSTONE.createBlockData();
    } else if (data.getMaterial() == Material.GRAVEL) {
      return Material.STONE.createBlockData();
    } else if (data.getMaterial() == Material.COARSE_DIRT) {
      return Material.DIRT.createBlockData();
    } else {
      return MaterialUtil.softType(data);
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 8000;
    @Modifiable(Attribute.CHARGE_TIME)
    private long chargeTime = 2500;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 3;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 1.2;
    @Modifiable(Attribute.RANGE)
    private double coneRange = 14;
    @Modifiable(Attribute.RANGE)
    private double ringRange = 9;
    private double fallThreshold = 12;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "shockwave");
    }
  }
}
