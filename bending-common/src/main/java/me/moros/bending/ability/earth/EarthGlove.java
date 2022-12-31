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

package me.moros.bending.ability.earth;

import java.util.Collection;
import java.util.List;

import me.moros.bending.BendingProperties;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.predicate.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.predicate.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.Platform;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.entity.EntityType;
import me.moros.bending.platform.entity.LivingEntity;
import me.moros.bending.platform.item.Inventory;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemSnapshot;
import me.moros.bending.platform.sound.Sound;
import me.moros.bending.platform.sound.SoundEffect;
import me.moros.bending.util.KeyUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.metadata.Metadata;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

// TODO possible changes: add per glove cooldown
public class EarthGlove extends AbilityInstance {
  enum Side {RIGHT, LEFT}

  private static final Config config = ConfigManager.load(Config::new);

  private static final double GLOVE_SPEED = 1.2;
  private static final double GLOVE_GRABBED_SPEED = 0.6;

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Entity glove;
  private Vector3d location;
  private Vector3d lastVelocity;
  private LivingEntity grabbedTarget;

  private boolean isMetal = false;
  private boolean returning = false;
  private boolean grabbed = false;

  public EarthGlove(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.SNEAK) {
      tryDestroy(user);
      return false;
    }

    if (user.onCooldown(description()) || user.game().abilityManager(user.worldUid()).userInstances(user, EarthGlove.class).count() >= 2) {
      return false;
    }

    this.user = user;
    loadConfig();

    if (launchEarthGlove()) {
      removalPolicy = Policies.builder()
        .add(Policies.UNDER_WATER)
        .add(Policies.UNDER_LAVA)
        .add(SwappedSlotsRemovalPolicy.of(description()))
        .add(OutOfRangeRemovalPolicy.of(userConfig.range + 5, () -> location))
        .build();
      user.addCooldown(description(), userConfig.cooldown);
      return true;
    }

    return false;
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

    if (glove == null || !glove.valid()) {
      return UpdateResult.REMOVE;
    }

    location = glove.location();
    if (location.distanceSq(user.eyeLocation()) > userConfig.range * userConfig.range) {
      returning = true;
    }

    if (!user.canBuild(glove.block())) {
      shatterGlove();
      return UpdateResult.REMOVE;
    }
    double factor = isMetal ? BendingProperties.instance().metalModifier() : 1;
    if (returning) {
      if (!user.sneaking()) {
        shatterGlove();
        return UpdateResult.REMOVE;
      }
      Vector3d returnLocation = user.eyeLocation().add(user.direction().multiply(isMetal ? 5 : 1.5));
      if (location.distanceSq(returnLocation) < 1) {
        if (grabbed && grabbedTarget != null) {
          grabbedTarget.applyVelocity(this, Vector3d.ZERO);
        }
        return UpdateResult.REMOVE;
      }
      if (grabbed) {
        if (!isValidTarget()) {
          shatterGlove();
          return UpdateResult.REMOVE;
        }
        Vector3d dir = returnLocation.subtract(grabbedTarget.location()).normalize().multiply(GLOVE_GRABBED_SPEED);
        grabbedTarget.applyVelocity(this, dir);
        glove.teleport(grabbedTarget.eyeLocation().subtract(0, grabbedTarget.height() / 2, 0));
        return UpdateResult.CONTINUE;
      } else {
        Vector3d dir = returnLocation.subtract(location).normalize().multiply(GLOVE_SPEED * factor);
        updateGloveVelocity(dir);
      }
    } else {
      double velocityLimit = (grabbed ? GLOVE_GRABBED_SPEED : GLOVE_SPEED * factor) - 0.2;
      Vector3d gloveVelocity = glove.velocity();
      if (glove.isOnGround() || lastVelocity.angle(gloveVelocity) > Math.PI / 6 || gloveVelocity.lengthSq() < velocityLimit * velocityLimit) {
        shatterGlove();
        return UpdateResult.REMOVE;
      }

      updateGloveVelocity(lastVelocity.normalize().multiply(GLOVE_SPEED * factor));
      boolean sneaking = user.sneaking();
      boolean collided = CollisionUtil.handle(user, new Sphere(location, 0.8), this::onEntityHit, true, false, sneaking);
      if (collided && !grabbed) {
        return UpdateResult.REMOVE;
      }
    }
    return UpdateResult.CONTINUE;
  }

  private boolean isValidTarget() {
    if (grabbedTarget == null || !grabbedTarget.valid()) {
      return false;
    }
    return grabbedTarget.world().equals(user.world());
  }

  private boolean onEntityHit(Entity entity) {
    if (user.sneaking()) {
      return grabTarget((LivingEntity) entity);
    }
    double damage = isMetal ? BendingProperties.instance().metalModifier(userConfig.damage) : userConfig.damage;
    entity.damage(damage, user, description());
    shatterGlove();
    return false;
  }

  private boolean grabTarget(LivingEntity entity) {
    if (grabbed || grabbedTarget != null) {
      return false;
    }
    returning = true;
    grabbed = true;
    grabbedTarget = entity;
    glove.teleport(grabbedTarget.eyeLocation().subtract(0, grabbedTarget.height() / 2, 0));
    grabbedTarget.fallDistance(0);
    if (isMetal) {
      removalPolicy = Policies.builder()
        .add(Policies.UNDER_WATER)
        .add(Policies.UNDER_LAVA)
        .add(SwappedSlotsRemovalPolicy.of(description()))
        .add(OutOfRangeRemovalPolicy.of(userConfig.range + 5, () -> location))
        .add(ExpireRemovalPolicy.of(userConfig.grabDuration))
        .build();
    }
    return true;
  }

  private boolean launchEarthGlove() {
    var key = KeyUtil.bending("glove-side", Side.class);
    Side side = user.store().toggle(key, Side.RIGHT);
    Vector3d gloveSpawnLocation = user.handSide(side == Side.RIGHT);
    Vector3d target = user.rayTrace(userConfig.range).cast(user.world()).entityCenterOrPosition();
    glove = buildGlove(gloveSpawnLocation.subtract(0, 0.2, 0));

    if (isMetal) {
      SoundEffect.METAL.play(user.world(), gloveSpawnLocation);
    } else {
      Sound.BLOCK_STONE_BREAK.asEffect(1, 1.5F).play(user.world(), gloveSpawnLocation);
    }

    double factor = isMetal ? BendingProperties.instance().metalModifier() : 1;
    Vector3d velocity = target.subtract(gloveSpawnLocation).normalize().multiply(GLOVE_SPEED * factor);
    updateGloveVelocity(velocity);
    location = glove.location();
    return true;
  }

  private Entity buildGlove(Vector3d spawnLocation) {
    Inventory inv = user.inventory();
    isMetal = user.hasPermission("bending.metal") && inv != null && inv.has(Item.IRON_INGOT);
    ItemSnapshot item = Platform.instance().factory().itemBuilder(isMetal ? Item.IRON_INGOT : Item.STONE).build();
    Entity entity = user.world().dropItem(spawnLocation, item, isMetal);
    entity.invulnerable(true);
    entity.gravity(false);
    entity.addMetadata(Metadata.GLOVE_KEY, this);
    if (isMetal && inv != null && inv.remove(Item.IRON_INGOT)) {
      return entity;
    }
    entity.addMetadata(Metadata.NO_PICKUP);
    return entity;
  }

  private void updateGloveVelocity(Vector3d velocity) {
    glove.applyVelocity(this, velocity);
    lastVelocity = glove.velocity();
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @Override
  public void onDestroy() {
    if (glove != null) {
      glove.remove();
    }
  }

  @Override
  public Collection<Collider> colliders() {
    return (glove == null || returning) ? List.of() : List.of(new Sphere(location, 0.8));
  }

  public void shatterGlove() {
    if (!glove.valid()) {
      return;
    }
    BlockType type = isMetal ? BlockType.IRON_BLOCK : BlockType.STONE;
    type.asParticle(location).count(6).offset(0.1).spawn(user.world());
    onDestroy();
  }

  private static void tryDestroy(User user) {
    CollisionUtil.handle(user, new Sphere(user.eyeLocation(), 8), entity -> {
      if (entity.type() == EntityType.ITEM && user.entity().hasLineOfSight(entity)) {
        EarthGlove ability = entity.metadata(Metadata.GLOVE_KEY, EarthGlove.class).findAny().orElse(null);
        if (ability != null && !user.equals(ability.user())) {
          ability.shatterGlove();
        }
      }
      return true;
    }, false, false);
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 750;
    @Modifiable(Attribute.RANGE)
    private double range = 16;
    @Comment("The maximum amount of milliseconds that the target will be controlled when grabbed by metal clips")
    @Modifiable(Attribute.DURATION)
    private long grabDuration = 4000;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 1;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "earth", "earthglove");
    }
  }
}
