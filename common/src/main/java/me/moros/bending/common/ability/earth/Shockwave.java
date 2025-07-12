/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.common.ability.earth;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.common.basic.BlockLine;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempDisplayEntity;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ExpiringSet;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;

public class Shockwave extends AbilityInstance {
  private static final Vector3d OFFSET = Vector3d.of(0.4, 0.85, 0.4);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final MultiUpdatable<Ripple> streams = MultiUpdatable.empty();
  private final Set<UUID> affectedEntities = new HashSet<>();
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
      user.game().abilityManager(user.worldKey()).firstInstance(user, Shockwave.class)
        .ifPresent(s -> s.release(true));
      return false;
    }

    if (user.game().abilityManager(user.worldKey()).hasAbility(user, Shockwave.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder().add(SwappedSlotsRemovalPolicy.of(description())).build();
    released = false;
    if (method == Activation.FALL) {
      if (user.propertyValue(EntityProperties.FALL_DISTANCE) < userConfig.fallThreshold || user.sneaking()) {
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
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (!released) {
      boolean charged = isCharged();
      if (charged) {
        Particle.SMOKE.builder(user.mainHandSide()).spawn(user.world());
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
      .map(b -> (Collider) AABB.BLOCK_BOUNDS.grow(OFFSET).at(b))
      .toList();
    if (!colliders.isEmpty()) {
      CollisionUtil.handle(user, Sphere.of(origin, range + 2), this::onEntityHit, false);
    }
    return streams.update();
  }

  private boolean onEntityHit(Entity entity) {
    if (!affectedEntities.contains(entity.uuid())) {
      Vector3d loc = entity.location();
      AABB entityCollider = entity.bounds();
      for (Collider aabb : colliders) {
        if (aabb.intersects(entityCollider)) {
          affectedEntities.add(entity.uuid());
          entity.damage(userConfig.damage, user, description());
          double deltaY = Math.min(0.8, 0.4 + loc.distance(origin) / (1.5 * range));
          Vector3d push = loc.subtract(origin).normalize().withY(deltaY).multiply(userConfig.knockback);
          entity.applyVelocity(this, push);
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

    origin = user.location().center();
    Vector3d dir = user.direction().withY(0).normalize();
    if (cone) {
      double deltaAngle = Math.PI / (3 * range);
      VectorUtil.createArc(dir, Vector3d.PLUS_J, deltaAngle, FastMath.ceil(range / 1.5)).forEach(v ->
        streams.add(new Ripple(Ray.of(origin, v.multiply(range)), 0))
      );
    } else {
      VectorUtil.circle(dir, Vector3d.PLUS_J, FastMath.ceil(6 * range)).forEach(v ->
        streams.add(new Ripple(Ray.of(origin, v.multiply(range)), 75))
      );
    }

    // First update in same tick to only apply cooldown if there are valid ripples
    if (streams.update() == UpdateResult.REMOVE) {
      return false;
    }
    removalPolicy = Policies.defaults();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
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
      if (block.type().isLiquid() || !MaterialUtil.isTransparent(block)) {
        return false;
      }
      return EarthMaterials.isEarthbendable(user, block.offset(Direction.DOWN));
    }

    @Override
    public void render(Block block) {
      if (!affectedBlocks.add(block)) {
        return;
      }
      recentAffectedBlocks.forceAdd(block);
      WorldUtil.tryBreakPlant(block);
      if (MaterialUtil.isFire(block)) {
        block.setType(BlockType.AIR);
      }
      double deltaY = Math.min(0.38, 0.18 + distanceTravelled / (3 * range));
      Block below = block.offset(Direction.DOWN);
      int blockLight = user.world().blockLightLevel(block);
      int skyLight = user.world().skyLightLevel(block);
      BlockState data = below.state();
      TempDisplayEntity.builder(data).gravity(true).velocity(Vector3d.of(0, deltaY, 0)).duration(1200)
        .edit(c -> c.brightness(blockLight, skyLight)).build(below);
      data.asParticle(block.center().add(0, deltaY, 0)).count(5)
        .offset(0.5, 0.25, 0.5).spawn(user.world());
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.EARTH.play(block);
      }
    }
  }

  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "earth", "shockwave");
    }
  }
}
