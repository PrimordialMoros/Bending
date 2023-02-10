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

package me.moros.bending.common.ability.earth;

import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.item.Inventory;
import me.moros.bending.api.platform.item.InventoryUtil;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempEntity.TempFallingBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.OutOfRangeRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class MetalCable extends AbilityInstance {
  public static final DataKey<MetalCable> CABLE_KEY = KeyUtil.data("metal-cable", MetalCable.class);

  private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(Vector3d.of(0.25, 0.25, 0.25));
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Collection<Vector3d> pointLocations;
  private Vector3d location;
  private Vector3d offset;
  private Entity cable;
  private CableTarget target;
  private TempFallingBlock projectile;

  private boolean hasHit = false;
  private boolean launched = false;
  private int ticks;

  public MetalCable(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.SNEAK) {
      for (Entity entity : user.world().nearbyEntities(user.eyeLocation(), 3, e -> e.type() == EntityType.ARROW)) {
        MetalCable ability = entity.get(CABLE_KEY).orElse(null);
        if (ability != null && !entity.uuid().equals(ability.user().uuid())) {
          ability.remove();
        }
      }
      return false;
    } else if (method == Activation.ATTACK) {
      for (var cable : user.game().abilityManager(user.worldKey()).userInstances(user, MetalCable.class).toList()) {
        if (!cable.launched) {
          cable.tryLaunchTarget();
          return false;
        }
      }
    }

    if (user.onCooldown(description())) {
      return false;
    }

    this.user = user;
    loadConfig();

    return launchCable();
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
    ticks++;
    if (launched) {
      return updateProjectile();
    }
    if (cable == null || !cable.valid()) {
      return UpdateResult.REMOVE;
    }
    location = cable.location();
    double distance = user.location().distance(location);
    if (hasHit) {
      if (!handleMovement(distance)) {
        return UpdateResult.REMOVE;
      }
    }
    return visualizeLine(distance) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private UpdateResult updateProjectile() {
    if (projectile == null || !projectile.valid()) {
      return UpdateResult.REMOVE;
    }
    location = projectile.center();
    if (ticks % 4 == 0) {
      if (CollisionUtil.handle(user, BOX.at(location), this::onProjectileHit)) {
        projectile.state().asParticle(location).count(8).offset(0.25, 0.15, 0.25).spawn(user.world());
        return UpdateResult.REMOVE;
      }
    }
    return UpdateResult.CONTINUE;
  }

  private boolean onProjectileHit(Entity entity) {
    BlockType mat = projectile.state().type();
    double damage;
    if (EarthMaterials.METAL_BENDABLE.isTagged(mat)) {
      damage = BendingProperties.instance().metalModifier(userConfig.damage);
    } else if (EarthMaterials.LAVA_BENDABLE.isTagged(mat)) {
      damage = BendingProperties.instance().magmaModifier(userConfig.damage);
    } else {
      damage = userConfig.damage;
    }
    entity.damage(damage, user, description());
    return true;
  }

  private boolean handleMovement(double distance) {
    if (target == null || !target.isValid(user)) {
      return false;
    }
    Entity entityToMove = user;
    Vector3d targetLocation = location;
    if (target.type == Type.ENTITY) {
      //noinspection DataFlowIssue
      cable.teleport(target.entity.location().add(0, target.offset, 0));
      if (user.sneaking()) {
        entityToMove = target.entity;
        Ray ray = user.ray(distance / 2);
        targetLocation = ray.origin.add(ray.direction);
      }
    }
    Vector3d direction = targetLocation.subtract(entityToMove.location()).normalize();
    if (distance > 3) {
      entityToMove.applyVelocity(this, direction.multiply(userConfig.pullSpeed));
    } else {
      if (target.type == Type.ENTITY) {
        entityToMove.applyVelocity(this, Vector3d.ZERO);
        if (projectile != null) {
          projectile.state().asParticle(projectile.center()).count(8).offset(0.25, 0.15, 0.25).spawn(user.world());
          projectile.remove();
          return false;
        }
        if (target.entity != null && target.entity.type() == EntityType.FALLING_BLOCK) {
          target.entity.remove();
        }
        return false;
      } else {
        if (distance > 1.5) {
          entityToMove.applyVelocity(this, direction.multiply(0.4 * userConfig.pullSpeed));
        } else {
          entityToMove.applyVelocity(this, Vector3d.of(0, 0.5, 0));
          return false;
        }
      }
    }
    return true;
  }

  private boolean launchCable() {
    if (!hasRequiredInv()) {
      return false;
    }

    Vector3d targetLocation = user.rayTrace(userConfig.range).cast(user.world()).entityCenterOrPosition();
    if (user.world().blockAt(targetLocation).type().isLiquid()) {
      return false;
    }

    Vector3d origin = user.mainHandSide();
    Vector3d dir = targetLocation.subtract(origin).normalize();
    Entity arrow = user.shootArrow(origin, dir, 1.8);
    arrow.add(CABLE_KEY, this);
    cable = arrow;
    location = cable.location();
    SoundEffect.METAL.play(user.world(), origin);

    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(OutOfRangeRemovalPolicy.of(userConfig.range, origin, () -> location))
      .build();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  private boolean visualizeLine(double distance) {
    Vector3d origin = user.mainHandSide();
    Vector3d dir = location.subtract(origin);
    Block ignore = target == null ? null : target.block;
    if (dir.lengthSq() > 0.1 && user.rayTrace(origin, dir).ignoreLiquids(false).ignore(ignore).blocks(user.world()).hit()) {
      return false;
    }
    boolean evenTicks = ticks % 2 == 0;
    if (!evenTicks) {
      int points = FastMath.ceil(distance * 2);
      offset = dir.multiply(1.0 / points);
      pointLocations = IntStream.rangeClosed(0, points - 1).mapToObj(i -> origin.add(offset.multiply(i))).toList();
    }
    Vector3d offset2 = evenTicks ? Vector3d.ZERO : offset.multiply(0.5);
    for (Vector3d temp : pointLocations) {
      ParticleBuilder.rgb(temp.add(offset2), "#444444", 0.75F).spawn(user.world());
    }
    return true;
  }

  public void hitBlock(Block block) {
    if (target != null) {
      return;
    }
    if (!user.canBuild(block)) {
      remove();
      return;
    }
    Vector3d dir = user.eyeLocation().subtract(location).normalize();
    if (user.sneaking() && !MaterialUtil.isUnbreakable(block)) {
      BlockState state = block.state();
      TempBlock.air().duration(BendingProperties.instance().earthRevertTime()).build(block);
      projectile = TempFallingBlock.fallingBlock(state).velocity(dir.multiply(0.2)).buildReal(block.world(), location);
      target = new CableTarget(projectile, 0.5);
    } else {
      dir = dir.negate();
      target = new CableTarget(block);
    }
    FragileStructure.tryDamageStructure(block, 2, new Ray(location, dir));
    hasHit = true;
  }

  public void hitEntity(Entity entity) {
    if (target != null || entity.uuid().equals(user.uuid())) {
      return;
    }
    if (!user.canBuild(entity.block())) {
      remove();
      return;
    }
    double offset = FastMath.clamp(cable.location().y() - entity.location().y(), 0, entity.height());
    target = new CableTarget(entity, offset);
    entity.fallDistance(0);
    hasHit = true;
  }

  private boolean hasRequiredInv() {
    if (InventoryUtil.hasMetalArmor(user)) {
      return true;
    }
    Inventory inv = user.inventory();
    return inv != null && inv.has(Item.IRON_INGOT);
  }

  private void remove() {
    removalPolicy = (u, d) -> true; // Remove in next tick
  }

  private void tryLaunchTarget() {
    if (launched || target == null || target.type == Type.BLOCK || target.entity == null) {
      return;
    }

    launched = true;
    Vector3d targetLocation = user.rayTrace(userConfig.projectileRange).cast(user.world()).entityCenterOrPosition();

    Vector3d velocity = targetLocation.subtract(location).normalize().multiply(userConfig.launchSpeed);
    target.entity.applyVelocity(this, velocity.add(0, 0.2, 0));
    target.entity.fallDistance(0);
    if (target.entity.type() == EntityType.FALLING_BLOCK) {
      removalPolicy = Policies.builder()
        .add(OutOfRangeRemovalPolicy.of(userConfig.projectileRange, location, () -> location))
        .build();
      onDestroy();
    } else {
      remove();
    }
  }

  @Override
  public void onDestroy() {
    if (cable != null) {
      cable.remove();
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public Collection<Collider> colliders() {
    if (launched && projectile != null) {
      return List.of(BOX.at(projectile.center()));
    }
    return List.of(new Sphere(location, 0.8));
  }

  private enum Type {ENTITY, BLOCK}

  private record CableTarget(Type type, @Nullable Entity entity, @Nullable Block block, @Nullable BlockType material,
                             double offset) {
    private CableTarget(Entity entity, double offset) {
      this(Type.ENTITY, entity, null, null, offset);
    }

    private CableTarget(Block block) {
      this(Type.BLOCK, null, block, block.type(), 0);
    }

    public boolean isValid(User u) {
      if (type == Type.ENTITY) {
        return entity != null && entity.valid() && entity.world().equals(u.world());
      } else {
        //noinspection DataFlowIssue
        return block.type() == material;
      }
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 4500;
    @Modifiable(Attribute.RANGE)
    private double range = 20;
    @Modifiable(Attribute.RANGE)
    private double projectileRange = 48;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2.5;
    @Modifiable(Attribute.SPEED)
    private double pullSpeed = 0.9;
    @Modifiable(Attribute.SPEED)
    private double launchSpeed = 1.6;

    @Override
    public List<String> path() {
      return List.of("abilities", "earth", "metalcable");
    }
  }
}
