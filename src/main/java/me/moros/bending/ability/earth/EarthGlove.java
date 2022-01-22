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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
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
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

// TODO possible changes: add per glove cooldown
public class EarthGlove extends AbilityInstance {
  public enum Side {RIGHT, LEFT}

  private static final Config config = new Config();

  private static final Map<UUID, Side> lastUsedSide = new HashMap<>();
  private static final double GLOVE_SPEED = 1.2;
  private static final double GLOVE_GRABBED_SPEED = 0.6;

  private static final ItemStack STONE = new ItemStack(Material.STONE, 1);
  private static final ItemStack INGOT = new ItemStack(Material.IRON_INGOT, 1);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private Item glove;
  private Vector3d location;
  private Vector3d lastVelocity;
  private LivingEntity grabbedTarget;

  private boolean isMetal = false;
  private boolean returning = false;
  private boolean grabbed = false;

  public EarthGlove(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (method == Activation.SNEAK) {
      tryDestroy(user);
      return false;
    }

    if (user.onCooldown(description()) || Bending.game().abilityManager(user.world()).userInstances(user, EarthGlove.class).count() >= 2) {
      return false;
    }

    this.user = user;
    loadConfig();

    if (launchEarthGlove()) {
      removalPolicy = Policies.builder()
        .add(Policies.IN_LIQUID)
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
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (glove == null || !glove.isValid()) {
      return UpdateResult.REMOVE;
    }

    location = new Vector3d(glove.getLocation());
    if (location.distanceSq(user.eyeLocation()) > userConfig.range * userConfig.range) {
      returning = true;
    }

    if (!user.canBuild(glove.getLocation().getBlock())) {
      shatterGlove();
      return UpdateResult.REMOVE;
    }
    double factor = isMetal ? BendingProperties.METAL_MODIFIER : 1;
    if (returning) {
      if (!user.sneaking()) {
        shatterGlove();
        return UpdateResult.REMOVE;
      }
      Vector3d returnLocation = user.eyeLocation().add(user.direction().multiply(isMetal ? 5 : 1.5));
      if (location.distanceSq(returnLocation) < 1) {
        if (grabbed && grabbedTarget != null) {
          EntityUtil.applyVelocity(this, grabbedTarget, Vector3d.ZERO);
        }
        return UpdateResult.REMOVE;
      }
      if (grabbed) {
        if (!isValidTarget()) {
          shatterGlove();
          return UpdateResult.REMOVE;
        }
        Vector3d dir = returnLocation.subtract(new Vector3d(grabbedTarget.getLocation())).normalize().multiply(GLOVE_GRABBED_SPEED);
        EntityUtil.applyVelocity(this, grabbedTarget, dir);
        glove.teleport(grabbedTarget.getEyeLocation().subtract(0, grabbedTarget.getHeight() / 2, 0));
        return UpdateResult.CONTINUE;
      } else {
        Vector3d dir = returnLocation.subtract(location).normalize().multiply(GLOVE_SPEED * factor);
        updateGloveVelocity(dir);
      }
    } else {
      double velocityLimit = (grabbed ? GLOVE_GRABBED_SPEED : GLOVE_SPEED * factor) - 0.2;
      Vector3d gloveVelocity = new Vector3d(glove.getVelocity());
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
    if (grabbedTarget == null || !grabbedTarget.isValid()) {
      return false;
    }
    if (grabbedTarget instanceof Player player && !player.isOnline()) {
      return false;
    }
    return grabbedTarget.getWorld().equals(user.world());
  }

  private boolean onEntityHit(Entity entity) {
    if (user.sneaking()) {
      return grabTarget((LivingEntity) entity);
    }
    double damage = isMetal ? BendingProperties.METAL_MODIFIER * userConfig.damage : userConfig.damage;
    DamageUtil.damageEntity(entity, user, damage, description());
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
    glove.teleport(grabbedTarget.getEyeLocation().subtract(0, grabbedTarget.getHeight() / 2, 0));
    grabbedTarget.setFallDistance(0);
    if (isMetal) {
      removalPolicy = Policies.builder()
        .add(Policies.IN_LIQUID)
        .add(SwappedSlotsRemovalPolicy.of(description()))
        .add(OutOfRangeRemovalPolicy.of(userConfig.range + 5, () -> location))
        .add(ExpireRemovalPolicy.of(userConfig.grabDuration))
        .build();
    }
    return true;
  }

  private boolean launchEarthGlove() {
    Side side = lastUsedSide.merge(user.uuid(), Side.RIGHT, (s1, s2) -> s1 == Side.RIGHT ? Side.LEFT : Side.RIGHT);
    Vector3d gloveSpawnLocation = user.handSide(side == Side.RIGHT);
    Vector3d target = user.compositeRayTrace(userConfig.range).result(user.world()).entityCenterOrPosition();
    glove = buildGlove(gloveSpawnLocation);

    if (isMetal) {
      SoundUtil.METAL.play(user.world(), gloveSpawnLocation);
    } else {
      SoundUtil.of(Sound.BLOCK_STONE_BREAK, 1, 1.5F).play(user.world(), gloveSpawnLocation);
    }

    double factor = isMetal ? BendingProperties.METAL_MODIFIER : 1;
    Vector3d velocity = target.subtract(gloveSpawnLocation).normalize().multiply(GLOVE_SPEED * factor);
    updateGloveVelocity(velocity);
    location = new Vector3d(glove.getLocation());
    return true;
  }

  private Item buildGlove(Vector3d spawnLocation) {
    isMetal = user.hasPermission("bending.metal") && InventoryUtil.hasItem(user, INGOT);
    Item item = user.world().dropItem(spawnLocation.toLocation(user.world()), isMetal ? INGOT : STONE);
    item.setInvulnerable(true);
    item.setGravity(false);
    item.setMetadata(Metadata.GLOVE_KEY, Metadata.of(this));
    if (isMetal && InventoryUtil.removeItem(user, INGOT)) {
      return item;
    }
    item.setCanMobPickup(false);
    item.setCanPlayerPickup(false);
    item.setMetadata(Metadata.NO_PICKUP, Metadata.empty());
    return item;
  }

  private void updateGloveVelocity(Vector3d velocity) {
    EntityUtil.applyVelocity(this, glove, velocity);
    lastVelocity = new Vector3d(glove.getVelocity());
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return (glove == null || returning) ? List.of() : List.of(new Sphere(location, 0.8));
  }

  public void shatterGlove() {
    if (!glove.isValid()) {
      return;
    }
    BlockData data = isMetal ? Material.IRON_BLOCK.createBlockData() : Material.STONE.createBlockData();
    ParticleUtil.of(Particle.BLOCK_CRACK, location).count(3).offset(0.1).data(data).spawn(user.world());
    ParticleUtil.of(Particle.BLOCK_DUST, location).count(2).offset(0.1).data(data).spawn(user.world());
    onDestroy();
  }

  private static void tryDestroy(@NonNull User user) {
    CollisionUtil.handle(user, new Sphere(user.eyeLocation(), 8), entity -> {
      if (entity instanceof Item && user.entity().hasLineOfSight(entity) && entity.hasMetadata(Metadata.GLOVE_KEY)) {
        EarthGlove ability = (EarthGlove) entity.getMetadata(Metadata.GLOVE_KEY).get(0).value();
        if (ability != null && !user.equals(ability.user())) {
          ability.shatterGlove();
        }
      }
      return true;
    }, false, false);
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.DURATION)
    public long grabDuration;
    @Modifiable(Attribute.DAMAGE)
    public double damage;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "earth", "earthglove");

      cooldown = abilityNode.node("cooldown").getLong(750);
      range = abilityNode.node("range").getDouble(16.0);
      grabDuration = abilityNode.node("duration").getLong(4000);
      damage = abilityNode.node("damage").getDouble(1.0);

      abilityNode.node("duration").comment("The maximum amount of milliseconds that the target will be controlled when grabbed by metal clips.");
    }
  }
}
