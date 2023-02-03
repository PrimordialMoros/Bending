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

package me.moros.bending.common.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.TravellingSource;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.ability.state.StateChain;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.functional.ExpireRemovalPolicy;
import me.moros.bending.api.functional.Policies;
import me.moros.bending.api.functional.RemovalPolicy;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.ExpiringSet;
import me.moros.bending.api.util.WorldUtil;
import me.moros.bending.api.util.collision.CollisionUtil;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import me.moros.bending.common.ability.water.sequence.WaterGimbal;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class WaterRing extends AbilityInstance {
  private static final double RING_RADIUS = 2.8;

  private static final Config config = ConfigManager.load(Config::new);
  private static AbilityDescription ringDesc;
  private static AbilityDescription waveDesc;

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Position lastPosition;
  private StateChain states;
  private final List<Block> ring = new ArrayList<>(24);
  private final Collection<IceShard> shards = new ArrayList<>(16);
  private final ExpiringSet<Entity> affectedEntities = new ExpiringSet<>(500);

  private boolean ready = false;
  private boolean completed = false;
  private boolean destroyed = false;
  private double radius = RING_RADIUS;
  private int index = 0;
  private int sources = 0;
  private long nextShardTime = 0;
  private long ringNextShrinkTime = 0;
  private long sneakStartTime = 0;

  public WaterRing(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, WaterGimbal.class)) {
      return false;
    }
    Optional<WaterRing> ring = user.game().abilityManager(user.worldKey()).firstInstance(user, WaterRing.class);
    if (ring.isPresent()) {
      if (method == Activation.ATTACK && user.selectedAbilityName().equals("WaterRing")) {
        if (user.sneaking()) {
          user.game().abilityManager(user.worldKey()).destroyInstance(ring.get());
        } else {
          ring.get().launchShard();
        }
      }
      return false;
    }

    this.user = user;
    loadConfig();
    Block source = user.find(userConfig.selectRange, WaterMaterials::isFullWaterSource);
    if (source == null) {
      return false;
    }
    List<Block> list = new ArrayList<>();
    list.add(source);
    states = new StateChain(list)
      .addState(new TravellingSource(user, BlockType.WATER.defaultState(), RING_RADIUS - 0.5, userConfig.selectRange + 5))
      .start();

    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(userConfig.duration)).build();

    if (waveDesc == null) {
      waveDesc = Objects.requireNonNull(Registries.ABILITIES.fromString("WaterWave"));
    }

    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  public List<Block> complete() {
    if (!ready || completed) {
      return List.of();
    }
    completed = true;
    sources = 0;
    int i = getDirectionIndex();
    if (i == 0) {
      return ring;
    }
    return Stream.concat(ring.subList(i, ring.size()).stream(), ring.subList(0, i).stream()).toList();
  }

  private Block getClosestRingBlock() {
    Vector3d dir = user.direction().withY(0).normalize().multiply(radius);
    Block target = user.world().blockAt(user.eyeBlock().center().add(dir));
    Block result = ring.get(0);
    Vector3d targetVector = target.toVector3d();
    double minDistance = Double.MAX_VALUE;
    for (Block block : ring) {
      if (target.equals(block)) {
        return target;
      }
      double d = block.distanceSq(targetVector);
      if (d < minDistance) {
        minDistance = d;
        result = block;
      }
    }
    return result;
  }

  private int getDirectionIndex() {
    Vector3d dir = user.direction().withY(0).normalize().multiply(radius);
    Block target = user.world().blockAt(user.eyeBlock().center().add(dir));
    return Math.max(0, ring.indexOf(target));
  }

  @Override
  public UpdateResult update() {
    if (completed || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (!ready) {
      if (states.update() == UpdateResult.REMOVE) {
        if (states.completed() && !states.chainStore().isEmpty()) {
          ring.addAll(WorldUtil.createBlockRing(user.eyeBlock(), this.radius));
          sources = ring.size();
          ready = true;
        } else {
          return UpdateResult.REMOVE;
        }
      }
      return UpdateResult.CONTINUE;
    }
    cleanAll();
    if (sources <= 0 || !user.canBuild()) {
      return UpdateResult.REMOVE;
    }
    Position newPosition = user.location().toVector3i();
    if (!newPosition.equals(lastPosition)) {
      ring.clear();
      ring.addAll(WorldUtil.createBlockRing(user.eyeBlock(), this.radius));
      Collections.rotate(ring, index);
      lastPosition = newPosition;
    }

    if (user.sneaking() && !user.selectedAbilityName().equals("OctopusForm")) {
      long time = System.currentTimeMillis();
      if (sneakStartTime == 0) {
        sneakStartTime = time;
        ringNextShrinkTime = time + 250;
      } else {
        if (ringNextShrinkTime > time && radius > 1.3) {
          radius(radius - 0.3);
          ringNextShrinkTime = time + 250;
        }
        if (time > sneakStartTime + userConfig.waveChargeTime && !user.onCooldown(waveDesc)) {
          if (!complete().isEmpty()) {
            user.game().activationController().activateAbility(user, Activation.SNEAK, waveDesc);
          }
          return UpdateResult.REMOVE;
        }
      }
    } else {
      sneakStartTime = 0;
      if (radius < RING_RADIUS) {
        radius(Math.min(radius + 0.3, RING_RADIUS));
      }
    }

    if (ring.stream().noneMatch(b -> user.canBuild(b))) {
      return UpdateResult.REMOVE;
    }
    Collections.rotate(ring, 1);
    index = ++index % ring.size();
    int length = Math.min(ring.size(), FastMath.ceil(sources * 0.8));
    for (int i = 0; i < length; i++) {
      Block block = ring.get(i);
      if (MaterialUtil.isWater(block) && !TempBlock.MANAGER.isTemp(block)) {
        ParticleBuilder.bubble(block).spawn(user.world());
      } else if (MaterialUtil.isTransparent(block)) {
        TempBlock.water().duration(250).build(block);
      }
    }

    if (userConfig.affectEntities) {
      CollisionUtil.handle(user, new Sphere(user.eyeLocation(), radius + 2), this::checkCollisions, false);
    }

    shards.removeIf(shard -> shard.update() == UpdateResult.REMOVE);
    return UpdateResult.CONTINUE;
  }

  private boolean checkCollisions(Entity entity) {
    for (Block block : ring) {
      if (affectedEntities.contains(entity)) {
        return false;
      }
      AABB blockBounds = AABB.BLOCK_BOUNDS.at(block);
      AABB entityBounds = entity.bounds();
      if (MaterialUtil.isWater(block) && !blockBounds.intersects(entityBounds)) {
        entity.damage(userConfig.damage, user, description());
        Vector3d velocity = entity.location().subtract(user.eyeLocation()).withY(0).normalize();
        entity.applyVelocity(WaterRing.this, velocity.multiply(userConfig.knockback));
        affectedEntities.add(entity);
      }
    }
    return false;
  }

  public boolean isReady() {
    return ready;
  }

  public boolean isDestroyed() {
    return destroyed;
  }

  public void radius(double radius) {
    if (radius < 1 || radius > 8 || this.radius == radius) {
      return;
    }
    this.radius = radius;
    cleanAll();
    ring.clear();
    ring.addAll(WorldUtil.createBlockRing(user.eyeBlock(), this.radius));
  }

  private void cleanAll() {
    ring.stream().filter(MaterialUtil::isWater).forEach(TempBlock.air()::build);
  }

  @Override
  public void onDestroy() {
    destroyed = true;
    if (!completed) {
      cleanAll();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  public static @Nullable WaterRing getOrCreateInstance(User user) {
    if (ringDesc == null) {
      ringDesc = Objects.requireNonNull(Registries.ABILITIES.fromString("WaterRing"));
    }
    WaterRing oldRing = user.game().abilityManager(user.worldKey()).firstInstance(user, WaterRing.class)
      .orElse(null);
    if (oldRing == null) {
      Ability newRing = user.game().activationController().activateAbility(user, Activation.ATTACK, ringDesc);
      if (newRing != null) {
        return (WaterRing) newRing;
      }
    }
    return oldRing;
  }

  private void launchShard() {
    if (!user.canBend(description()) || ring.isEmpty()) {
      return;
    }
    long time = System.currentTimeMillis();
    if (time >= nextShardTime) {
      nextShardTime = time + userConfig.shardCooldown;
      Vector3d origin = getClosestRingBlock().toVector3d();
      Vector3d lookingDir = user.direction().multiply(userConfig.shardRange + radius);
      shards.add(new IceShard(new Ray(origin, lookingDir)));
    }
  }

  private class IceShard extends ParticleStream {
    public IceShard(Ray ray) {
      super(user, ray, 0.3, 0.5);
      canCollide = BlockType::isLiquid;
      steps = 5;
    }

    @Override
    public void render() {
      Particle.ITEM_SNOWBALL.builder(location).count(3).offset(0.25).spawn(user.world());
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.ICE.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      entity.damage(userConfig.shardDamage, user, description());
      return true;
    }

    @Override
    public boolean onBlockHit(Block block) {
      return WorldUtil.tryCoolLava(user, block);
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.DURATION)
    private long duration = 30000;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 16;
    private boolean affectEntities = true;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 1;
    @Modifiable(Attribute.STRENGTH)
    private double knockback = 1;
    // Shards
    @Modifiable(Attribute.COOLDOWN)
    private long shardCooldown = 1000;
    @Modifiable(Attribute.RANGE)
    private double shardRange = 16;
    @Modifiable(Attribute.DAMAGE)
    private double shardDamage = 0.25;
    // Wave
    @Modifiable(Attribute.CHARGE_TIME)
    private long waveChargeTime = 750;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "waterring");
    }
  }
}
