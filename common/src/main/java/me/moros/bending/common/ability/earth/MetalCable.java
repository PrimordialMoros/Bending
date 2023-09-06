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
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.FragileStructure;
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
import me.moros.bending.api.platform.particle.Particle;
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
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.ability.earth.util.Projectile;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class MetalCable extends AbilityInstance {
  public static final DataKey<MetalCable> CABLE_KEY = KeyUtil.data("metal-cable", MetalCable.class);

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Vector3d location;
  private Entity cable;
  private Attached<?> attached;

  private boolean hasHit = false;
  private int ticks;

  public MetalCable(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.SNEAK) {
      for (Entity entity : user.world().nearbyEntities(user.eyeLocation(), 3, e -> e.type() == EntityType.ARROW)) {
        MetalCable ability = entity.get(CABLE_KEY).orElse(null);
        if (ability != null && !user.uuid().equals(ability.user().uuid())) {
          ability.remove();
        }
      }
      return false;
    } else if (method == Activation.ATTACK) {
      for (var cable : user.game().abilityManager(user.worldKey()).userInstances(user, MetalCable.class).toList()) {
        cable.tryLaunchTarget();
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
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    ticks++;
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

  private boolean handleMovement(double distance) {
    if (attached == null || !attached.isValid(user)) {
      return false;
    }
    Entity entityToMove = user;
    Vector3d targetLocation = location;
    AttachedEntity attachedEntity = null;
    if (attached instanceof AttachedEntity temp) {
      attachedEntity = temp;
      cable.teleport(attachedEntity.handle().location().add(0, attachedEntity.offset(), 0));
      if (user.sneaking()) {
        entityToMove = attachedEntity.handle();
        Ray ray = user.ray(distance / 2);
        targetLocation = ray.position().add(ray.direction());
      }
    }
    Vector3d direction = targetLocation.subtract(entityToMove.location()).normalize();
    if (distance > 3) {
      entityToMove.applyVelocity(this, direction.multiply(userConfig.pullSpeed));
    } else {
      if (attachedEntity != null) {
        entityToMove.applyVelocity(this, Vector3d.ZERO);
        if (attachedEntity.handle().type() == EntityType.FALLING_BLOCK) {
          if (attachedEntity.handle() instanceof TempFallingBlock fallingBlock) {
            fallingBlock.state().asParticle(fallingBlock.center()).count(8)
              .offset(0.25, 0.15, 0.25).spawn(user.world());
          }
          attachedEntity.handle().remove();
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
    Block ignore = Optional.ofNullable(attached)
      .filter(AttachedBlock.class::isInstance).map(AttachedBlock.class::cast)
      .map(AttachedBlock::handle).orElse(null);
    if (dir.lengthSq() > 0.1 && user.rayTrace(origin, dir).ignoreLiquids(false).ignore(ignore).blocks(user.world()).hit()) {
      return false;
    }
    int points = FastMath.ceil(distance * 2);
    Vector3d offset = dir.multiply(1.0 / points);
    Vector3d originWithOffset = origin.add(offset.multiply(0.33 * (ticks % 3)));
    for (int i = 0; i < points; i++) {
      ParticleBuilder.rgb(originWithOffset.add(offset.multiply(i)), "#444444", 0.75F).spawn(user.world());
    }
    return true;
  }

  public void hitBlock(Block block) {
    if (attached != null) {
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
      var entity = TempFallingBlock.fallingBlock(state).velocity(dir.multiply(0.2)).buildReal(block.world(), location);
      var ability = new Projectile(user, description(), entity, userConfig.projectileRange, userConfig.damage);
      user.game().abilityManager(user.worldKey()).addAbility(ability);
      attached = new AttachedEntity(entity, 0.5);
    } else {
      dir = dir.negate();
      attached = new AttachedBlock(block, block.type());
    }
    FragileStructure.tryDamageStructure(block, 2, Ray.of(location, dir));
    hasHit = true;
  }

  public void hitEntity(Entity entity) {
    if (attached != null || entity.uuid().equals(user.uuid())) {
      return;
    }
    if (!user.canBuild(entity.block())) {
      remove();
      return;
    }
    double offset = FastMath.clamp(cable.location().y() - entity.location().y(), 0, entity.height());
    attached = new AttachedEntity(entity, offset);
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
    if (attached == null || !attached.canLaunch()) {
      return;
    }
    if (attached instanceof AttachedEntity attachedEntity) {
      Vector3d targetLocation = user.rayTrace(userConfig.projectileRange).cast(user.world()).entityCenterOrPosition();
      Vector3d velocity = targetLocation.subtract(location).normalize().multiply(userConfig.launchSpeed);
      attachedEntity.handle().applyVelocity(this, velocity.add(0, 0.2, 0));
      attachedEntity.handle().fallDistance(0);
    }
    attached = null;
    remove();
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
    Vector3d origin = user.mainHandSide();
    Vector3d dir = location.subtract(origin);
    return List.of(Ray.of(origin, dir), Sphere.of(location, 0.8));
  }

  public Optional<Entity> electrify(Vector3d pos, boolean directed) {
    Vector3d origin = user.mainHandSide();
    Vector3d projected = VectorUtil.closestPoint(origin, location, pos);
    Vector3d dirToOrigin = origin.subtract(projected);
    Vector3d dirToEnd = location.subtract(projected);
    if (directed || dirToOrigin.lengthSq() < dirToEnd.lengthSq()) {
      visualizeElectrifiedLine(projected, dirToOrigin);
      return Optional.of(user);
    } else {
      visualizeElectrifiedLine(projected, dirToEnd);
      return Optional.ofNullable(attached).filter(AttachedEntity.class::isInstance)
        .map(AttachedEntity.class::cast).map(AttachedEntity::handle);
    }
  }

  private void visualizeElectrifiedLine(Vector3d origin, Vector3d direction) {
    int points = FastMath.ceil(direction.length() * 4);
    Vector3d offset = direction.multiply(1.0 / points);
    for (int i = 0; i < points; i++) {
      Vector3d v = origin.add(offset.multiply(i));
      Particle.WAX_OFF.builder(v).offset(0.05).spawn(user.world());
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.LIGHTNING.play(user.world(), v);
        Particle.ELECTRIC_SPARK.builder(v).offset(0.1).count(5).spawn(user.world());
      }
    }
  }

  private interface Attached<T> {
    T handle();

    boolean isValid(User user);

    default boolean canLaunch() {
      return true;
    }
  }

  private record AttachedEntity(Entity handle, double offset) implements Attached<Entity> {
    @Override
    public boolean isValid(User user) {
      return handle.valid() && handle.world().equals(user.world());
    }
  }

  private record AttachedBlock(Block handle, BlockType material) implements Attached<Block> {
    @Override
    public boolean isValid(User user) {
      return handle.type() == material;
    }

    @Override
    public boolean canLaunch() {
      return false;
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
