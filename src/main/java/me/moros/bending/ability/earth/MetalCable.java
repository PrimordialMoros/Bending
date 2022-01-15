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
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.IntStream;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.game.temporal.TempFallingBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.AABB;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.InventoryUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.EarthMaterials;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class MetalCable extends AbilityInstance {
  private static final AABB BOX = AABB.BLOCK_BOUNDS.grow(new Vector3d(0.25, 0.25, 0.25));

  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Collection<Vector3d> pointLocations;
  private Vector3d location;
  private Vector3d offset;
  private Arrow cable;
  private CableTarget target;
  private TempFallingBlock projectile;

  private boolean hasHit = false;
  private boolean launched = false;
  private int ticks;

  public MetalCable(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (!user.hasPermission("bending.metal")) {
      return false;
    }

    if (method == Activation.SNEAK) {
      Location center = user.entity().getEyeLocation();
      Predicate<Entity> predicate = e -> e.hasMetadata(Metadata.METAL_CABLE);
      for (Entity entity : center.getNearbyEntitiesByType(Arrow.class, 3, predicate)) {
        MetalCable ability = (MetalCable) entity.getMetadata(Metadata.METAL_CABLE).get(0).value();
        if (ability != null && !entity.equals(ability.user().entity())) {
          ability.remove();
        }
      }
      return false;
    } else if (method == Activation.ATTACK) {
      Optional<MetalCable> cable = Bending.game().abilityManager(user.world()).firstInstance(user, MetalCable.class);
      if (cable.isPresent()) {
        cable.get().tryLaunchTarget();
        return false;
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    ticks++;
    if (launched) {
      return updateProjectile();
    }
    if (cable == null || !cable.isValid()) {
      return UpdateResult.REMOVE;
    }
    location = new Vector3d(cable.getLocation());
    double distance = user.location().distance(location);
    if (hasHit) {
      if (!handleMovement(distance)) {
        return UpdateResult.REMOVE;
      }
    }
    return visualizeLine(distance) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private UpdateResult updateProjectile() {
    if (projectile == null || !projectile.fallingBlock().isValid()) {
      return UpdateResult.REMOVE;
    }
    location = projectile.center();
    if (ticks % 4 == 0) {
      if (CollisionUtil.handle(user, BOX.at(location), this::onProjectileHit)) {
        BlockData bd = projectile.fallingBlock().getBlockData();
        Location bukkitLocation = location.toLocation(user.world());
        ParticleUtil.of(Particle.BLOCK_CRACK, bukkitLocation).count(4)
          .offset(0.25, 0.15, 0.25).data(bd).spawn();
        ParticleUtil.of(Particle.BLOCK_DUST, bukkitLocation).count(6)
          .offset(0.25, 0.15, 0.25).data(bd).spawn();
        return UpdateResult.REMOVE;
      }
    }
    return UpdateResult.CONTINUE;
  }

  private boolean onProjectileHit(Entity entity) {
    Material mat = projectile.fallingBlock().getBlockData().getMaterial();
    double damage = userConfig.damage;
    if (EarthMaterials.METAL_BENDABLE.isTagged(mat)) {
      damage *= BendingProperties.METAL_MODIFIER;
    } else if (EarthMaterials.LAVA_BENDABLE.isTagged(mat)) {
      damage *= BendingProperties.MAGMA_MODIFIER;
    }
    DamageUtil.damageEntity(entity, user, damage, description());
    return true;
  }

  private boolean handleMovement(double distance) {
    if (target == null || !target.isValid(user)) {
      return false;
    }
    Entity entityToMove = user.entity();
    Vector3d targetLocation = location;
    if (target.type == CableTarget.Type.ENTITY) {
      if (target.entity != null) {
        cable.teleport(target.entity.getLocation());
      }
      if (user.sneaking() || projectile != null) {
        entityToMove = target.entity;
        Ray ray = user.ray(distance / 2);
        targetLocation = ray.origin.add(ray.direction);
      }
    }
    Vector3d direction = targetLocation.subtract(new Vector3d(entityToMove.getLocation())).normalize();
    if (distance > 3) {
      EntityUtil.applyVelocity(this, entityToMove, direction.multiply(userConfig.pullSpeed));
    } else {
      if (target.type == CableTarget.Type.ENTITY) {
        EntityUtil.applyVelocity(this, entityToMove, Vector3d.ZERO);
        if (target.entity instanceof FallingBlock fb) {
          Location tempLocation = fb.getLocation().add(0, 0.5, 0);
          ParticleUtil.of(Particle.BLOCK_CRACK, tempLocation).count(4)
            .offset(0.25, 0.15, 0.25).data(fb.getBlockData()).spawn();
          ParticleUtil.of(Particle.BLOCK_DUST, tempLocation).count(6)
            .offset(0.25, 0.15, 0.25).data(fb.getBlockData()).spawn();
          target.entity.remove();
        }
        return false;
      } else {
        if (distance <= 3 && distance > 1.5) {
          EntityUtil.applyVelocity(this, entityToMove, direction.multiply(0.4 * userConfig.pullSpeed));
        } else {
          EntityUtil.applyVelocity(this, entityToMove, new Vector3d(0, 0.5, 0));
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

    Vector3d targetLocation = user.compositeRayTrace(userConfig.range).result(user.world()).entityCenterOrPosition();
    if (targetLocation.toBlock(user.world()).isLiquid()) {
      return false;
    }

    Vector3d origin = user.mainHandSide();
    Vector3d dir = targetLocation.subtract(origin).normalize();
    Arrow arrow = user.world().spawnArrow(origin.toLocation(user.world()), dir.toBukkitVector(), 1.8F, 0);
    arrow.setShooter(user.entity());
    arrow.setGravity(false);
    arrow.setInvulnerable(true);
    arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
    arrow.setMetadata(Metadata.METAL_CABLE, Metadata.of(this));
    cable = arrow;
    location = new Vector3d(cable.getLocation());
    SoundUtil.METAL.play(arrow.getLocation());

    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(OutOfRangeRemovalPolicy.of(userConfig.range, origin, () -> location))
      .build();
    user.addCooldown(description(), userConfig.cooldown);
    return true;
  }

  private boolean visualizeLine(double distance) {
    boolean evenTicks = ticks % 2 == 0;
    if (!evenTicks) {
      int points = FastMath.ceil(distance * 2);
      Vector3d origin = user.mainHandSide();
      offset = location.subtract(origin).multiply(1.0 / points);
      pointLocations = IntStream.rangeClosed(0, points - 1).mapToObj(i -> origin.add(offset.multiply(i))).toList();
    }
    int counter = 0;
    Vector3d offset2 = offset.multiply(0.5);
    for (Vector3d temp : pointLocations) {
      Block block = temp.toBlock(user.world());
      if (block.isLiquid() || !MaterialUtil.isTransparent(block)) {
        if (++counter > 2) {
          return false;
        }
      }
      Vector3d spawnLoc = evenTicks ? temp : temp.add(offset2);
      ParticleUtil.rgb(spawnLoc.toLocation(user.world()), "444444", 0.75F).spawn();
    }
    return true;
  }

  public void hitBlock(@NonNull Block block) {
    if (target != null) {
      return;
    }
    if (!user.canBuild(block)) {
      remove();
      return;
    }
    if (user.sneaking() && !MaterialUtil.isUnbreakable(block)) {
      BlockData data = block.getBlockData();
      TempBlock.air().duration(BendingProperties.EARTHBENDING_REVERT_TIME).build(block);
      Vector3d velocity = user.eyeLocation().subtract(location).normalize().multiply(0.2);
      projectile = TempFallingBlock.builder(data).velocity(velocity).build(user.world(), location);
      target = new CableTarget(projectile.fallingBlock());
    } else {
      target = new CableTarget(block);
    }
    hasHit = true;
  }

  public void hitEntity(@NonNull Entity entity) {
    if (target != null) {
      return;
    }
    if (!user.canBuild(entity.getLocation().getBlock())) {
      remove();
      return;
    }
    target = new CableTarget(entity);
    entity.setFallDistance(0);
    hasHit = true;
  }

  private boolean hasRequiredInv() {
    if (InventoryUtil.hasMetalArmor(user.entity())) {
      return true;
    }
    return InventoryUtil.hasItem(user, new ItemStack(Material.IRON_INGOT, 1));
  }

  private void remove() {
    removalPolicy = (u, d) -> true; // Remove in next tick
  }

  private void tryLaunchTarget() {
    if (launched || target == null || target.type == CableTarget.Type.BLOCK || target.entity == null) {
      return;
    }

    launched = true;
    Vector3d targetLocation = user.compositeRayTrace(userConfig.projectileRange).result(user.world()).entityCenterOrPosition();

    Vector3d velocity = targetLocation.subtract(location).normalize().multiply(userConfig.launchSpeed);
    EntityUtil.applyVelocity(this, target.entity, velocity.add(new Vector3d(0, 0.2, 0)));
    target.entity.setFallDistance(0);
    if (target.entity instanceof FallingBlock) {
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    if (launched && projectile != null) {
      return List.of(BOX.at(projectile.center()));
    }
    return List.of(new Sphere(location, 0.8));
  }

  private static final class CableTarget {
    private enum Type {ENTITY, BLOCK}

    private final Type type;
    private final Entity entity;
    private final Block block;
    private final Material material;

    private CableTarget(Entity entity) {
      block = null;
      material = null;
      this.entity = entity;
      type = Type.ENTITY;
    }

    private CableTarget(Block block) {
      entity = null;
      this.block = block;
      material = block.getType();
      type = Type.BLOCK;
    }

    public boolean isValid(User u) {
      if (type == Type.ENTITY) {
        return entity != null && entity.isValid() && entity.getWorld().equals(u.world());
      } else {
        return block.getType() == material;
      }
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.RANGE)
    public double projectileRange;
    @Modifiable(Attribute.DAMAGE)
    private double damage;
    @Modifiable(Attribute.SPEED)
    private double pullSpeed;
    @Modifiable(Attribute.SPEED)
    private double launchSpeed;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "metalcable");

      cooldown = abilityNode.node("cooldown").getLong(4500);
      range = abilityNode.node("range").getDouble(28.0);
      projectileRange = abilityNode.node("projectile-range").getDouble(48.0);
      damage = abilityNode.node("damage").getDouble(2.5);
      pullSpeed = abilityNode.node("pull-speed").getDouble(0.9);
      launchSpeed = abilityNode.node("launch-speed").getDouble(1.6);
    }
  }
}
