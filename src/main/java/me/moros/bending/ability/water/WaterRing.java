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

package me.moros.bending.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.TravellingSource;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.ability.water.sequences.WaterGimbal;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.math.Vector3i;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ExpiringSet;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.AABBUtils;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WaterRing extends AbilityInstance {
  public static final double RING_RADIUS = 2.8;

  private static final Config config = new Config();
  private static AbilityDescription ringDesc;
  private static AbilityDescription waveDesc;

  private User user;
  private Config userConfig;

  private RemovalPolicy removalPolicy;
  private Vector3i lastPosition;
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

  public WaterRing(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, WaterGimbal.class)) {
      return false;
    }
    Optional<WaterRing> ring = Bending.game().abilityManager(user.world()).firstInstance(user, WaterRing.class);
    if (ring.isPresent()) {
      if (method == Activation.ATTACK && user.selectedAbilityName().equals("WaterRing")) {
        if (user.sneaking()) {
          Bending.game().abilityManager(user.world()).destroyInstance(ring.get());
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
      .addState(new TravellingSource(user, Material.WATER.createBlockData(), RING_RADIUS - 0.5, userConfig.selectRange + 5))
      .start();

    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(userConfig.duration)).build();

    if (waveDesc == null) {
      waveDesc = Objects.requireNonNull(Registries.ABILITIES.ability("WaterWave"));
    }

    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  public @NonNull List<@NonNull Block> complete() {
    if (!ready || completed) {
      return List.of();
    }
    completed = true;
    sources = 0;
    int i = getDirectionIndex();
    if (i == 0) {
      return ring;
    }
    return Stream.concat(ring.subList(i, ring.size()).stream(), ring.subList(0, i).stream()).collect(Collectors.toList());
  }

  private Block getClosestRingBlock() {
    Vector3d dir = user.direction().setY(0).normalize().multiply(radius);
    Block target = Vector3d.center(user.headBlock()).add(dir).toBlock(user.world());
    Block result = ring.get(0);
    Vector3d targetVector = new Vector3d(target);
    double minDistance = Double.MAX_VALUE;
    for (Block block : ring) {
      if (target.equals(block)) {
        return target;
      }
      double d = new Vector3d(block).distanceSq(targetVector);
      if (d < minDistance) {
        minDistance = d;
        result = block;
      }
    }
    return result;
  }

  private int getDirectionIndex() {
    Vector3d dir = user.direction().setY(0).normalize().multiply(radius);
    Block target = Vector3d.center(user.headBlock()).add(dir).toBlock(user.world());
    return Math.max(0, ring.indexOf(target));
  }

  @Override
  public @NonNull UpdateResult update() {
    if (completed || removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (!ready) {
      if (states.update() == UpdateResult.REMOVE) {
        if (states.completed() && !states.chainStore().isEmpty()) {
          ring.addAll(BlockMethods.createBlockRing(user.headBlock(), this.radius));
          sources = ring.size();
          ready = true;
        } else {
          return UpdateResult.REMOVE;
        }
      }
      return UpdateResult.CONTINUE;
    }
    cleanAll();
    if (sources <= 0 || !user.canBuild(user.headBlock())) {
      return UpdateResult.REMOVE;
    }
    Vector3i newPosition = user.location().toVector3i();
    if (!newPosition.equals(lastPosition)) {
      ring.clear();
      ring.addAll(BlockMethods.createBlockRing(user.headBlock(), this.radius));
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
        if (time > sneakStartTime + userConfig.chargeTime && !user.onCooldown(waveDesc)) {
          if (!complete().isEmpty()) {
            Bending.game().activationController().activateAbility(user, Activation.SNEAK, waveDesc);
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

    for (int i = 0; i < Math.min(ring.size(), FastMath.ceil(sources * 0.8)); i++) {
      Block block = ring.get(i);
      if (MaterialUtil.isWater(block) && !TempBlock.MANAGER.isTemp(block)) {
        ParticleUtil.createBubble(block).spawn();
      } else if (MaterialUtil.isTransparent(block)) {
        TempBlock.create(block, Material.WATER.createBlockData(), 250);
      }
    }

    if (userConfig.affectEntities) {
      CollisionUtil.handleEntityCollisions(user, new Sphere(user.eyeLocation(), radius + 2), this::checkCollisions, false);
    }

    shards.removeIf(shard -> shard.update() == UpdateResult.REMOVE);
    return UpdateResult.CONTINUE;
  }

  private boolean checkCollisions(Entity entity) {
    for (Block block : ring) {
      if (affectedEntities.contains(entity)) {
        return false;
      }
      AABB blockBounds = AABB.BLOCK_BOUNDS.at(new Vector3d(block));
      AABB entityBounds = AABBUtils.entityBounds(entity);
      if (MaterialUtil.isWater(block) && !blockBounds.intersects(entityBounds)) {
        DamageUtil.damageEntity(entity, user, userConfig.damage, description());
        Vector3d velocity = new Vector3d(entity.getLocation()).subtract(user.eyeLocation()).setY(0).normalize();
        EntityMethods.applyVelocity(WaterRing.this, entity, velocity.multiply(userConfig.knockback));
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

  public double radius() {
    return radius;
  }

  public void radius(double radius) {
    if (radius < 1 || radius > 8 || this.radius == radius) {
      return;
    }
    this.radius = radius;
    cleanAll();
    ring.clear();
    ring.addAll(BlockMethods.createBlockRing(user.headBlock(), this.radius));
  }

  private void cleanAll() {
    ring.stream().filter(MaterialUtil::isWater).forEach(TempBlock::createAir);
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

  public static @Nullable WaterRing getOrCreateInstance(@NonNull User user) {
    if (ringDesc == null) {
      ringDesc = Objects.requireNonNull(Registries.ABILITIES.ability("WaterRing"));
    }
    WaterRing oldRing = Bending.game().abilityManager(user.world()).firstInstance(user, WaterRing.class)
      .orElse(null);
    if (oldRing == null) {
      Ability newRing = Bending.game().activationController().activateAbility(user, Activation.ATTACK, ringDesc);
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
      nextShardTime = time + userConfig.cooldown;
      Vector3d origin = new Vector3d(getClosestRingBlock());
      Vector3d lookingDir = user.direction().multiply(userConfig.shardRange + radius);
      shards.add(new IceShard(new Ray(origin, lookingDir)));
    }
  }

  private class IceShard extends ParticleStream {
    public IceShard(Ray ray) {
      super(user, ray, 0.3, 0.5);
      canCollide = Block::isLiquid;
      steps = 5;
    }

    @Override
    public void render() {
      ParticleUtil.create(Particle.SNOW_SHOVEL, bukkitLocation()).count(3)
        .offset(0.25, 0.25, 0.25).spawn();
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.ICE.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      DamageUtil.damageEntity(entity, user, userConfig.shardDamage, description());
      return true;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      return BlockMethods.tryCoolLava(user, block);
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.DURATION)
    public long duration;
    @Modifiable(Attribute.SELECTION)
    public double selectRange;
    public boolean affectEntities;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.STRENGTH)
    public double knockback;
    // Shards
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double shardRange;
    @Modifiable(Attribute.DAMAGE)
    public double shardDamage;
    // Wave
    @Modifiable(Attribute.CHARGE_TIME)
    public long chargeTime;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "waterring");

      duration = abilityNode.node("duration").getLong(30000);
      selectRange = abilityNode.node("select-range").getDouble(16.0);
      affectEntities = abilityNode.node("affect-entities").getBoolean(true);
      damage = abilityNode.node("damage").getDouble(1.0);
      knockback = abilityNode.node("knockback").getDouble(1.0);

      CommentedConfigurationNode shardsNode = abilityNode.node("shards");
      cooldown = shardsNode.node("cooldown").getLong(1000);
      shardRange = shardsNode.node("range").getDouble(16.0);
      shardDamage = shardsNode.node("damage").getDouble(0.25);

      CommentedConfigurationNode waveNode = abilityNode.node("waterwave");
      chargeTime = waveNode.node("charge-time").getLong(750);
    }
  }
}
