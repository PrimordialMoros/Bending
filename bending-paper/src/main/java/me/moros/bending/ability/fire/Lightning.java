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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import me.moros.bending.adapter.NativeAdapter;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.FragileStructure;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Rotation;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.raytrace.CompositeRayTrace;
import me.moros.bending.model.raytrace.RayTrace;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.temporal.TempLight;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.InventoryUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Lightning extends AbilityInstance {
  private static final double POINT_DISTANCE = 0.2;

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Set<Entity> affectedEntities = new HashSet<>();

  private ListIterator<LineSegment> arcIterator;

  private boolean launched = false;
  private boolean exploded = false;
  private double factor;
  private long startTime;

  public Lightning(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.world()).userInstances(user, Lightning.class).anyMatch(l -> !l.launched)) {
      return false;
    }
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder()
      .add(ExpireRemovalPolicy.of(userConfig.overchargeTime))
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();

    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || exploded) {
      return UpdateResult.REMOVE;
    }
    if (launched) {
      return advanceLightning() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
    }
    if (user.sneaking()) {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.LIGHTNING_CHARGING.play(user.world(), user.eyeLocation());
      }
      long deltaTime = System.currentTimeMillis() - startTime;
      if (deltaTime > userConfig.minChargeTime) {
        Vector3d spawnLoc = user.mainHandSide();
        double offset = deltaTime / (3.0 * userConfig.overchargeTime);
        ParticleUtil.rgb(spawnLoc, "01E1FF").offset(offset).spawn(user.world());
        if (deltaTime > userConfig.maxChargeTime) {
          ParticleUtil.of(Particle.ELECTRIC_SPARK, spawnLoc).spawn(user.world());
        }
      }
    } else {
      launch();
    }
    return UpdateResult.CONTINUE;
  }

  private void launch() {
    if (launched) {
      return;
    }

    long deltaTime = System.currentTimeMillis() - startTime;
    factor = 1;
    if (deltaTime >= userConfig.maxChargeTime) {
      factor = userConfig.chargeFactor;
    } else if (deltaTime >= userConfig.minChargeTime) {
      double deltaChargeTime = userConfig.maxChargeTime - userConfig.minChargeTime;
      double deltaFactor = (userConfig.chargeFactor - factor) * (deltaTime - userConfig.minChargeTime) / deltaChargeTime;
      factor += deltaFactor;
    } else {
      removalPolicy = (u, d) -> true; // Remove in next tick
      return;
    }

    double distance = userConfig.range * factor;
    Vector3d origin = user.eyeLocation();
    Vector3d target = origin.add(user.direction().multiply(distance));
    RayTrace rayTrace = user.rayTrace(distance).raySize(1.5).entities(user.world());
    Vector3d point = null;
    if (rayTrace.hit()) {
      point = rayTrace.position();
    }
    arcIterator = new Arc(origin, target, point).iterator();
    user.addCooldown(description(), userConfig.cooldown);
    removalPolicy = Policies.builder().build();
    launched = true;
  }

  private boolean advanceLightning() {
    double counter = 0;
    while (arcIterator.hasNext() && counter < userConfig.speed) {
      LineSegment segment = arcIterator.next();
      CompositeRayTrace result = (CompositeRayTrace) user.rayTrace(segment.start, segment.direction)
        .ignoreLiquids(true).raySize(0.3).entities(user.world());
      if (!segment.isFork) {
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
          SoundUtil.LIGHTNING.play(user.world(), segment.mid);
        }
        counter += segment.length;
        Block block = result.block();
        if (block != null) {
          if (NativeAdapter.instance().tryPowerLightningRod(block)) {
            return false;
          }
          explode(result.position(), block);
          return false;
        }
      }
      if (!user.canBuild(segment.end.toBlock(user.world()))) {
        return false;
      }
      renderSegment(segment);
      if (electrocuteAround(result.entity())) {
        return false;
      }
    }
    return arcIterator.hasNext();
  }

  private void renderSegment(LineSegment segment) {
    for (Vector3d v : segment) {
      ParticleUtil.of(Particle.WAX_OFF, v).spawn(user.world());
    }
    TempLight.builder(15).build(segment.mid.toBlock(user.world()));
  }

  private boolean handleRedirection(Iterable<Entity> entitiesToCheck) {
    for (Entity e : entitiesToCheck) {
      User bendingUser = Registries.BENDERS.get(e.getUniqueId());
      if (bendingUser != null) {
        Lightning other = user.game().abilityManager(user.world()).userInstances(bendingUser, Lightning.class)
          .filter(l -> !l.launched).findFirst().orElse(null);
        if (other != null) {
          other.startTime = 0;
          return true;
        }
      }
    }
    return false;
  }

  private boolean electrocuteAround(@Nullable Entity entity) {
    if (entity != null) {
      Collider collider = new Sphere(EntityUtil.entityCenter(entity), userConfig.radius);
      Collection<Entity> entities = new ArrayList<>();
      CollisionUtil.handle(user, collider, entities::add);
      boolean remove = handleRedirection(entities);
      if (!remove) {
        for (Entity e : entities) {
          if (onEntityHit(e)) {
            remove = true;
            ParticleUtil.of(Particle.ELECTRIC_SPARK, EntityUtil.entityCenter(e)).count(8).offset(0.3).spawn(user.world());
          }
        }
      }
      return remove;
    }
    return false;
  }

  private boolean onEntityHit(Entity entity) {
    if (!affectedEntities.contains(entity)) {
      affectedEntities.add(entity);
      if (entity instanceof Creeper creeper) {
        creeper.setPowered(true);
      }
      boolean hitWater = entity.isInWaterOrBubbleColumn();
      boolean grounded = EntityUtil.isOnGround(entity);
      boolean hasMetalArmor = entity instanceof LivingEntity livingEntity && InventoryUtil.hasMetalArmor(livingEntity);
      double dmgFactor = hitWater ? 2 : (grounded && hasMetalArmor ? 0.5 : 1);
      double damage = factor * dmgFactor * userConfig.damage;
      DamageUtil.damageEntity(entity, user, damage, description());
      if (grounded) {
        exploded = true; // Grounded, no explosion should happen
      }
      return true;
    }
    return false;
  }

  private boolean touchLiquid(Vector3d center, Block block) {
    Block centerBlock = center.toBlock(user.world());
    if (block.isLiquid() || centerBlock.isLiquid()) {
      return true;
    }
    if (WorldUtil.FACES.stream().map(block::getRelative).anyMatch(Block::isLiquid)) {
      return true;
    }
    return !centerBlock.equals(block) && WorldUtil.FACES.stream().map(centerBlock::getRelative).anyMatch(Block::isLiquid);
  }

  private void explode(Vector3d center, Block block) {
    if (exploded || touchLiquid(center, block)) {
      return;
    }
    exploded = true;
    if (WaterMaterials.isIceBendable(block)) {
      FragileStructure.tryDamageStructure(List.of(block), 0);
    }
    BendingExplosion.builder()
      .size(userConfig.explosionRadius)
      .damage(userConfig.explosionDamage)
      .fireTicks(0)
      .breakBlocks(true)
      .sound(6, 1)
      .buildAndExplode(this, center);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public void onDestroy() {
    if (!launched && userConfig.overchargeTime > 0 && System.currentTimeMillis() > startTime + userConfig.overchargeTime) {
      SoundUtil.LIGHTNING.play(user.world(), user.location());
      user.addCooldown(description(), userConfig.cooldown);
      DamageUtil.damageEntity(user.entity(), user, userConfig.overchargeDamage, description());
    }
  }

  private static final class Arc implements Iterable<LineSegment> {
    private final ThreadLocalRandom rand = ThreadLocalRandom.current();
    private final List<LineSegment> segments;
    private final Vector3d start;

    private static final double OFFSET = 1.6;
    private static final double FORK_CHANCE = 0.5;

    private Arc(Vector3d start, Vector3d end, @Nullable Vector3d target) {
      this.start = start;
      Function<LineSegment, Vector3d> f = target == null ? ls -> randomOffset(ls, 0.75 * OFFSET) : ls -> target;
      List<LineSegment> startingSegments = new LinkedList<>(displaceMidpoint(new LineSegment(start, end), f, 0));
      segments = generateRecursively(OFFSET, startingSegments, 0.25);
    }

    private List<LineSegment> displaceMidpoint(LineSegment segment, Function<LineSegment, Vector3d> function, double forkChance) {
      Vector3d offsetPoint = function.apply(segment);
      LineSegment first = new LineSegment(segment.start, offsetPoint, segment.isFork);
      LineSegment second = new LineSegment(offsetPoint, segment.end, segment.isFork);
      if (forkChance > 0 && rand.nextDouble() < forkChance) {
        Vector3d forkEnd = offsetPoint.add(randomDirection(offsetPoint.subtract(start), segment.length * 0.75));
        return List.of(first, new LineSegment(offsetPoint, forkEnd, true), second);
      }
      return List.of(first, second);
    }

    private List<LineSegment> generateRecursively(double maxOffset, List<LineSegment> lines, double maxSegmentLength) {
      int size = lines.size();
      ListIterator<LineSegment> it = lines.listIterator();
      while (it.hasNext()) {
        LineSegment toSplit = it.next();
        if ((toSplit.isFork && toSplit.length < 0.1) || (!toSplit.isFork && toSplit.length < maxSegmentLength)) {
          continue;
        }
        List<LineSegment> split = displaceMidpoint(toSplit, ls -> randomOffset(ls, maxOffset), FORK_CHANCE);
        it.remove();
        split.forEach(it::add);
      }
      if (size != lines.size()) {
        return generateRecursively(maxOffset * 0.5, lines, maxSegmentLength);
      }
      return List.copyOf(lines);
    }

    private Vector3d randomOffset(LineSegment segment, double maxOffset) {
      double length = maxOffset * 0.5 * (rand.nextGaussian() + 1);
      double angle = rand.nextDouble(2 * Math.PI);
      return segment.mid.add(VectorUtil.orthogonal(segment.direction, angle, length));
    }

    private Vector3d randomDirection(Vector3d axis, double maxLength) {
      Rotation rotation = new Rotation(axis, rand.nextDouble(Math.PI / 4));
      Vector3d angledVector = rand.nextBoolean() ? rotation.applyTo(axis) : rotation.applyInverseTo(axis);
      double halfLength = 0.5 * maxLength;
      return angledVector.normalize().multiply(halfLength + rand.nextDouble() * halfLength);
    }

    @Override
    public ListIterator<LineSegment> iterator() {
      return segments.listIterator();
    }
  }

  private static final class LineSegment implements Iterable<Vector3d> {
    private final Vector3d start;
    private final Vector3d end;
    private final Vector3d direction;
    private final Vector3d mid;
    private final double length;
    private final boolean isFork;

    private LineSegment(Vector3d start, Vector3d end) {
      this(start, end, false);
    }

    private LineSegment(Vector3d start, Vector3d end, boolean isFork) {
      this.start = start;
      this.end = end;
      this.isFork = isFork;
      Vector3d dir = end.subtract(start);
      direction = dir.normalize();
      length = dir.length();
      mid = start.add(direction.multiply(length * 0.5));
    }

    @Override
    public Iterator<Vector3d> iterator() {
      return new Iterator<>() {
        private double f = 0;

        @Override
        public boolean hasNext() {
          return f < length;
        }

        @Override
        public Vector3d next() {
          if (!hasNext()) {
            throw new RuntimeException("Reached segment end");
          }
          f += POINT_DISTANCE;
          return start.add(direction.multiply(f));
        }
      };
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 1.5;
    @Modifiable(Attribute.RANGE)
    private double range = 15;
    @Modifiable(Attribute.RADIUS)
    private double radius = 1.5;
    @Modifiable(Attribute.SPEED)
    private double speed = 2;
    @Modifiable(Attribute.CHARGE_TIME)
    private long minChargeTime = 1000;
    @Modifiable(Attribute.CHARGE_TIME)
    private long maxChargeTime = 3000;
    @Modifiable(Attribute.STRENGTH)
    private double chargeFactor = 2;

    @Modifiable(Attribute.DAMAGE)
    private double explosionDamage = 3;
    @Modifiable(Attribute.RADIUS)
    private double explosionRadius = 2.5;

    @Modifiable(Attribute.DURATION)
    private long overchargeTime = 8000;
    @Modifiable(Attribute.DAMAGE)
    private double overchargeDamage = 4;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "fire", "lightning");
    }
  }
}
