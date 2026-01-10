/*
 * Copyright 2020-2026 Moros
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

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.Platform;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.entity.EntityType;
import me.moros.bending.api.platform.entity.LivingEntity;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.item.ItemSnapshot;
import me.moros.bending.api.platform.item.PlayerInventory;
import me.moros.bending.api.platform.sound.Sound;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.FeaturePermissions;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.OutOfRangeRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.math.Vector3d;
import org.spongepowered.configurate.objectmapping.meta.Comment;

// TODO possible changes: add per glove cooldown
public class EarthGlove extends AbilityInstance {
  public static final DataKey<EarthGlove> GLOVE_KEY = KeyUtil.data("earth-glove", EarthGlove.class);

  private enum Side {RIGHT, LEFT}

  private static final DataKey<Side> KEY = KeyUtil.data("glove-side", Side.class);
  private static final double GLOVE_SPEED = 1.2;
  private static final double GLOVE_GRABBED_SPEED = 0.6;

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

    if (user.onCooldown(description()) || user.game().abilityManager(user.worldKey()).userInstances(user, EarthGlove.class).count() >= 2) {
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
    userConfig = user.game().configProcessor().calculate(this, Config.class);
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
      boolean onGround = glove.isOnGround();
      if (onGround || lastVelocity.angle(gloveVelocity) > Math.PI / 6 || gloveVelocity.lengthSq() < velocityLimit * velocityLimit) {
        if (onGround) {
          glove.velocity(Vector3d.ZERO);
        }
        shatterGlove();
        return UpdateResult.REMOVE;
      }

      updateGloveVelocity(lastVelocity.normalize().multiply(GLOVE_SPEED * factor));
      boolean sneaking = user.sneaking();
      boolean collided = CollisionUtil.handle(user, Sphere.of(location, 0.8), this::onEntityHit, true, false, sneaking);
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
    grabbedTarget.setProperty(EntityProperties.FALL_DISTANCE, 0D);
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
    Side side = user.store().toggle(KEY, Side.RIGHT);
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
    if (user.inventory() instanceof PlayerInventory inv) {
      isMetal = user.hasPermission(FeaturePermissions.METAL) && inv.remove(Item.IRON_INGOT);
    }
    ItemSnapshot item = Platform.instance().factory().itemBuilder(isMetal ? Item.IRON_INGOT : Item.STONE).build();
    Entity entity = user.world().dropItem(spawnLocation, item, false);
    entity.setProperty(EntityProperties.INVULNERABLE, true);
    entity.setProperty(EntityProperties.GRAVITY, isMetal);
    entity.add(GLOVE_KEY, this);
    return entity;
  }

  private void updateGloveVelocity(Vector3d velocity) {
    glove.applyVelocity(this, velocity);
    lastVelocity = glove.velocity();
  }

  @Override
  public void onDestroy() {
    if (glove != null) {
      if (isMetal) {
        glove.setProperty(EntityProperties.ALLOW_PICKUP, true);
      } else {
        glove.remove();
      }
    }
  }

  @Override
  public Collection<Collider> colliders() {
    return (glove == null || returning) ? List.of() : List.of(Sphere.of(location, 0.8));
  }

  public void shatterGlove() {
    if (!glove.valid()) {
      return;
    }
    if (!isMetal) {
      BlockType.STONE.asParticle(location).count(6).offset(0.1).spawn(user.world());
    }
    onDestroy();
  }

  private static void tryDestroy(User user) {
    Vector3d eyeLoc = user.eyeLocation();
    Vector3d dir = user.direction();
    CollisionUtil.handle(user, Sphere.of(user.eyeLocation(), 8), entity -> {
      if (entity.type() == EntityType.ITEM && entity.location().subtract(eyeLoc).angle(dir) < Math.PI / 3) {
        EarthGlove ability = entity.get(GLOVE_KEY).orElse(null);
        if (ability != null && !user.equals(ability.user())) {
          ability.shatterGlove();
        }
      }
      return true;
    }, false, false);
  }

  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "earth", "earthglove");
    }
  }
}
