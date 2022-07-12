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

package me.moros.bending.ability.air;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Collider;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.key.RegistryKey;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.OutOfRangeRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingEffect;
import me.moros.bending.util.ColorPalette;
import me.moros.bending.util.EntityUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.collision.AABBUtil;
import me.moros.bending.util.material.MaterialUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirBlast extends AbilityInstance {
  public enum Mode {PUSH, PULL}

  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private AirStream stream;
  private Vector3d origin;

  private boolean launched;
  private boolean selectedOrigin;

  public AirBlast(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    if (Policies.UNDER_WATER.test(user, description()) || Policies.UNDER_LAVA.test(user, description())) {
      return false;
    }

    removalPolicy = Policies.builder()
      .add(OutOfRangeRemovalPolicy.of(userConfig.selectRange * 2, () -> origin))
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
      .build();

    for (AirBlast blast : user.game().abilityManager(user.world()).userInstances(user, AirBlast.class).toList()) {
      if (!blast.launched) {
        if (method == Activation.SNEAK_RELEASE) {
          if (!blast.selectOrigin()) {
            user.game().abilityManager(user.world()).destroyInstance(blast);
          }
        } else {
          blast.launch();
        }
        return false;
      }
    }

    if (method == Activation.SNEAK_RELEASE) {
      return selectOrigin();
    }
    origin = user.eyeLocation();
    launch();
    return true;
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

    if (!launched) {
      if (!description().equals(user.selectedAbility())) {
        return UpdateResult.REMOVE;
      }
      ParticleUtil.air(origin).count(4).offset(0.5).spawn(user.world());
    }

    return (!launched || stream.update() == UpdateResult.CONTINUE) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private boolean selectOrigin() {
    Vector3d offset = user.direction().multiply(0.5);
    origin = user.rayTrace(userConfig.selectRange).ignoreLiquids(false).entities(user.world()).position().subtract(offset);
    selectedOrigin = true;
    return user.canBuild(origin.toBlock(user.world()));
  }

  private void launch() {
    launched = true;
    Vector3d target = user.rayTrace(userConfig.range).entities(user.world()).entityCenterOrPosition();
    if (user.store().getOrDefault(RegistryKey.create("airblast-mode", Mode.class), Mode.PUSH) == Mode.PULL) {
      Vector3d temp = new Vector3d(origin.toArray());
      origin = new Vector3d(target.toArray());
      target = temp;
    }
    Vector3d direction = target.subtract(origin).normalize();
    removalPolicy = Policies.builder().build();
    user.addCooldown(description(), userConfig.cooldown);
    stream = new AirStream(new Ray(origin, direction.multiply(userConfig.range)));
  }

  @Override
  public Collection<Collider> colliders() {
    return stream == null ? List.of() : List.of(stream.collider());
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  public static void switchMode(User user) {
    if (user.selectedAbilityName().equals("AirBlast")) {
      var key = RegistryKey.create("airblast-mode", Mode.class);
      if (user.store().canEdit(key)) {
        Mode mode = user.store().toggle(key, Mode.PUSH);
        user.sendActionBar(Component.text("Mode: " + mode.name(), ColorPalette.TEXT_COLOR));
      }
    }
  }

  private class AirStream extends ParticleStream {
    public AirStream(Ray ray) {
      super(user, ray, userConfig.speed, 1.3);
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b);
      livingOnly = false;
    }

    @Override
    public void render() {
      ParticleUtil.air(location).count(6).offset(0.275).spawn(user.world());
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.AIR.play(user.world(), location);
      }
      // Handle user separately from the general entity collision.
      if (selectedOrigin) {
        if (AABBUtil.entityBounds(user.entity()).intersects(new Sphere(location, 2))) {
          onEntityHit(user.entity());
        }
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      boolean isUser = entity.equals(user.entity());
      double factor = isUser ? userConfig.powerSelf : userConfig.powerOther;
      BendingEffect.FIRE_TICK.reset(entity);
      if (factor == 0) {
        return false;
      }

      Vector3d push = ray.direction.normalize();
      if (!isUser) {
        // Cap vertical push
        push = push.withY(Math.max(-0.3, Math.min(0.3, push.y())));
      }

      factor *= 1 - (distanceTravelled / (2 * maxRange));
      // Reduce the push if the player is on the ground.
      if (isUser && user.isOnGround()) {
        factor *= 0.5;
      }
      Vector3d velocity = new Vector3d(entity.getVelocity());
      // The strength of the entity's velocity in the direction of the blast.
      double strength = velocity.dot(push.normalize());
      if (strength > factor) {
        double f = velocity.normalize().dot(push.normalize());
        velocity = velocity.multiply(0.5).add(push.normalize().multiply(f));
      } else if (strength + factor * 0.5 > factor) {
        velocity = velocity.add(push.multiply(factor - strength));
      } else {
        velocity = velocity.add(push.multiply(factor * 0.5));
      }
      EntityUtil.applyVelocity(AirBlast.this, entity, velocity);
      entity.setFallDistance(0);
      return false;
    }

    @Override
    public boolean onBlockHit(Block block) {
      if (WorldUtil.tryExtinguishFire(user, block)) {
        return false;
      }
      WorldUtil.tryCoolLava(user, block);
      return true;
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 1250;
    @Modifiable(Attribute.RANGE)
    private double range = 20.0;
    @Modifiable(Attribute.SPEED)
    private double speed = 1.2;
    @Modifiable(Attribute.STRENGTH)
    private double powerSelf = 2.1;
    @Modifiable(Attribute.STRENGTH)
    private double powerOther = 2.1;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 8.0;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "air", "airblast");
    }
  }
}
