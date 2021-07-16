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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.FragileStructure;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Rotation;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.registry.Registries;
import me.moros.bending.util.BendingExplosion;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.InventoryUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.RayTrace.CompositeResult;
import me.moros.bending.util.RayTrace.Type;
import me.moros.bending.util.SoundEffect;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Lightning extends AbilityInstance {
  private static final double POINT_DISTANCE = 0.2;

  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Set<Entity> affectedEntities = new HashSet<>();

  private ListIterator<LineSegment> arcIterator;

  private boolean launched = false;
  private boolean exploded = false;
  private double factor;
  private long startTime;

  public Lightning(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).userInstances(user, Lightning.class).anyMatch(l -> !l.launched)) {
      return false;
    }
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder()
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .build();

    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description()) || exploded) {
      return UpdateResult.REMOVE;
    }
    if (launched) {
      advanceLightning();
      return advanceLightning() ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
    }
    if (user.sneaking()) {
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.LIGHTNING_CHARGING.play(user.entity().getEyeLocation());
      }
      long deltaTime = System.currentTimeMillis() - startTime;
      if (deltaTime > userConfig.minChargeTime) {
        Location spawnLoc = user.mainHandSide().toLocation(user.world());
        double offset = deltaTime / (3.0 * userConfig.duration);
        ParticleUtil.createRGB(spawnLoc, "01E1FF").offset(offset, offset, offset).spawn();
        if (deltaTime > userConfig.maxChargeTime) {
          ParticleUtil.create(Particle.END_ROD, spawnLoc).spawn();
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

    Vector3d origin = user.eyeLocation();
    Vector3d target = origin.add(user.direction().multiply(userConfig.range * factor));
    arcIterator = new Arc(origin, target).iterator();
    user.addCooldown(description(), userConfig.cooldown);
    removalPolicy = Policies.builder().build();
    launched = true;
  }

  private boolean advanceLightning() {
    double counter = 0;
    while (arcIterator.hasNext() && counter < userConfig.speed) {
      LineSegment segment = arcIterator.next();
      CompositeResult result = RayTrace.of(segment.start, segment.direction).range(segment.length)
        .type(Type.COMPOSITE).entityPredicate(this::isValidEntity).ignoreLiquids(false).raySize(0.3).result(user.world());
      if (!segment.isFork) {
        if (ThreadLocalRandom.current().nextInt(6) == 0) {
          SoundUtil.LIGHTNING.play(segment.mid.toLocation(user.world()));
        }
        counter += segment.length;
        Block block = result.block();
        if (block != null) {
          explode(result.position(), result.block());
          return false;
        }
      }
      if (!renderSegment(segment) || electrocuteAround(result.entity())) {
        return false;
      }
    }
    return arcIterator.hasNext();
  }

  private boolean renderSegment(LineSegment segment) {
    for (Vector3d v : segment) {
      Location loc = v.toLocation(user.world());
      if (!user.canBuild(loc.getBlock())) {
        return false;
      }
      ParticleUtil.create(Particle.END_ROD, v.toLocation(user.world())).spawn();
    }
    return true;
  }

  private boolean isValidEntity(Entity entity) {
    if (!(entity instanceof LivingEntity)) {
      return false;
    }
    if (entity instanceof Player && ((Player) entity).getGameMode() == GameMode.SPECTATOR) {
      return false;
    }
    return !entity.equals(user.entity());
  }

  private boolean handleRedirection(Collection<Entity> entitiesToCheck) {
    for (Entity e : entitiesToCheck) {
      if (e instanceof Player) {
        BendingPlayer bendingPlayer = Registries.BENDERS.user((Player) e);
        Lightning other = Bending.game().abilityManager(user.world()).userInstances(bendingPlayer, Lightning.class)
          .filter(l -> !l.launched).findFirst().orElse(null);
        if (other != null) {
          other.startTime = 0;
          return true;
        }
      }
    }
    return false;
  }

  private boolean electrocuteAround(Entity entity) {
    if (entity != null) {
      Collider collider = new Sphere(EntityMethods.entityCenter(entity), userConfig.radius);
      Collection<Entity> entities = new ArrayList<>();
      CollisionUtil.handleEntityCollisions(user, collider, entities::add);
      boolean remove = handleRedirection(entities);
      if (!remove) {
        for (Entity e : entities) {
          remove |= onEntityHit(e);
        }
      }
      return remove;
    }
    return false;
  }

  private boolean onEntityHit(Entity entity) {
    if (!affectedEntities.contains(entity)) {
      affectedEntities.add(entity);
      if (entity instanceof Creeper) {
        ((Creeper) entity).setPowered(true);
      }
      boolean hitWater = entity.isInWater();
      boolean grounded = EntityMethods.isOnGround(entity);
      boolean hasMetalArmor = entity instanceof LivingEntity && InventoryUtil.hasMetalArmor((LivingEntity) entity);
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

  private void explode(Vector3d center, Block block) {
    if (exploded) {
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
      .soundEffect(new SoundEffect(Sound.ENTITY_GENERIC_EXPLODE, 6, 1))
      .buildAndExplode(this, center);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public void onDestroy() {
    if (!launched && userConfig.duration > 0 && System.currentTimeMillis() > startTime + userConfig.duration) {
      SoundUtil.LIGHTNING.play(user.entity().getLocation(), 2, 0);
      user.addCooldown(description(), userConfig.cooldown);
      DamageUtil.damageEntity(user.entity(), user, userConfig.overchargeDamage, description());
    }
  }

  private static class Arc implements Iterable<LineSegment> {
    private final ThreadLocalRandom rand = ThreadLocalRandom.current();
    private final List<LineSegment> segments;
    private final Vector3d start;

    private static final double OFFSET = 1.6;
    private static final double FORK_CHANCE = 0.5;

    private Arc(Vector3d start, Vector3d end) {
      this.start = start;
      List<LineSegment> startingSegments = new LinkedList<>(displaceMidpoint(new LineSegment(start, end), 1.2, 0));
      segments = generateRecursively(OFFSET, startingSegments, 2, FastMath.ceil(4 * start.distance(end)));
    }

    private List<LineSegment> displaceMidpoint(LineSegment segment, double maxOffset, double forkChance) {
      Vector3d offsetPoint = randomOffset(segment, maxOffset);
      LineSegment first = new LineSegment(segment.start, offsetPoint, segment.isFork);
      LineSegment second = new LineSegment(offsetPoint, segment.end, segment.isFork);
      if (forkChance > 0 && rand.nextDouble() < forkChance) {
        Vector3d forkEnd = offsetPoint.add(randomDirection(offsetPoint.subtract(start), segment.length * 0.75));
        return List.of(first, new LineSegment(offsetPoint, forkEnd, true), second);
      }
      return List.of(first, second);
    }

    private List<LineSegment> generateRecursively(double maxOffset, List<LineSegment> lines, int counter, int maxAmount) {
      if (counter < maxAmount) {
        ListIterator<LineSegment> it = lines.listIterator();
        while (it.hasNext()) {
          LineSegment toSplit = it.next();
          if (toSplit.isFork && toSplit.length < 0.1) {
            continue;
          }
          List<LineSegment> split = displaceMidpoint(toSplit, maxOffset, FORK_CHANCE);
          it.remove();
          split.forEach(it::add);
        }
        return generateRecursively(maxOffset * 0.5, lines, 2 * counter, maxAmount);
      }
      return List.copyOf(lines);
    }

    private Vector3d randomOffset(LineSegment segment, double maxOffset) {
      double length = maxOffset * 0.5 * (rand.nextGaussian() + 1);
      double angle = rand.nextDouble(2 * Math.PI);
      return segment.mid.add(VectorMethods.orthogonal(segment.direction, angle, length));
    }

    private Vector3d randomDirection(Vector3d axis, double maxLength) {
      Rotation rotation = new Rotation(axis, rand.nextDouble(Math.PI / 4));
      Vector3d angledVector = rand.nextBoolean() ? rotation.applyTo(axis) : rotation.applyInverseTo(axis);
      double halfLength = 0.5 * maxLength;
      return angledVector.normalize().multiply(halfLength + rand.nextDouble() * halfLength);
    }

    @Override
    public @NonNull ListIterator<LineSegment> iterator() {
      return segments.listIterator();
    }
  }

  private static class LineSegment implements Iterable<Vector3d> {
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
    public @NonNull Iterator<Vector3d> iterator() {
      return new Iterator<>() {
        private double f = 0;

        @Override
        public boolean hasNext() {
          return f < length;
        }

        @Override
        public Vector3d next() {
          if (hasNext()) {
            f += POINT_DISTANCE;
            return start.add(direction.multiply(f));
          }
          return null;
        }
      };
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.RADIUS)
    public double radius;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.CHARGE_TIME)
    public long minChargeTime;
    @Modifiable(Attribute.CHARGE_TIME)
    public long maxChargeTime;
    @Modifiable(Attribute.STRENGTH)
    public double chargeFactor;

    @Modifiable(Attribute.DAMAGE)
    public double explosionDamage;
    @Modifiable(Attribute.RADIUS)
    public double explosionRadius;

    @Modifiable(Attribute.DURATION)
    public long duration;
    @Modifiable(Attribute.DAMAGE)
    public double overchargeDamage;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "lightning");
      cooldown = abilityNode.node("cooldown").getLong(6000);
      damage = abilityNode.node("damage").getDouble(1.5);
      range = abilityNode.node("range").getDouble(15.0);
      radius = abilityNode.node("radius").getDouble(1.5);
      speed = abilityNode.node("speed").getDouble(2.0);
      minChargeTime = abilityNode.node("min-charge-time").getLong(1000);
      maxChargeTime = abilityNode.node("max-charge-time").getLong(3000);
      chargeFactor = abilityNode.node("charge-factor").getDouble(2.0);

      explosionRadius = abilityNode.node("explosion-radius").getDouble(2.5);
      explosionDamage = abilityNode.node("explosion-damage").getDouble(3.0);

      duration = abilityNode.node("overcharge-duration").getLong(8000);
      overchargeDamage = abilityNode.node("overcharge-damage").getDouble(4.0);
    }
  }
}
