/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.ability.air;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.ColorPalette;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.bending.api.util.functional.OutOfRangeRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import net.kyori.adventure.text.Component;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class AirBlast extends AbilityInstance {
  private enum Mode {PUSH, PULL}

  private static final DataKey<Mode> KEY = KeyUtil.data("airblast-mode", Mode.class);

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

    for (AirBlast blast : user.game().abilityManager(user.worldKey()).userInstances(user, AirBlast.class).toList()) {
      if (!blast.launched) {
        if (method == Activation.SNEAK_RELEASE) {
          if (!blast.selectOrigin()) {
            user.game().abilityManager(user.worldKey()).destroyInstance(blast);
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
    userConfig = user.game().configProcessor().calculate(this, Config.class);
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
      ParticleBuilder.air(origin).count(4).offset(0.5).spawn(user.world());
      return UpdateResult.CONTINUE;
    }

    return stream.update();
  }

  private boolean selectOrigin() {
    Vector3d offset = user.direction().multiply(0.5);
    origin = user.rayTrace(userConfig.selectRange).ignoreLiquids(false).cast(user.world()).position().subtract(offset);
    selectedOrigin = true;
    return user.canBuild(origin);
  }

  private void launch() {
    launched = true;
    Vector3d target = user.rayTrace(userConfig.range).cast(user.world()).entityCenterOrPosition();
    if (user.store().get(KEY).orElse(Mode.PUSH) == Mode.PULL) {
      Vector3d temp = origin;
      origin = target;
      target = temp;
    }
    Vector3d direction = target.subtract(origin).normalize();
    removalPolicy = Policies.defaults();
    user.addCooldown(description(), userConfig.cooldown);
    stream = new AirStream(Ray.of(origin, direction.multiply(userConfig.range)));
  }

  @Override
  public Collection<Collider> colliders() {
    return stream == null ? List.of() : List.of(stream.collider());
  }

  public static void switchMode(User user) {
    if (user.hasAbilitySelected("airblast")) {
      if (user.store().canEdit(KEY)) {
        Mode mode = user.store().toggle(KEY, Mode.PUSH);
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
      ParticleBuilder.air(location).count(6).offset(0.275).spawn(user.world());
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.AIR.play(user.world(), location);
      }
      // Handle user separately from the general entity collision.
      if (selectedOrigin && user.bounds().intersects(Sphere.of(location, 2))) {
        onEntityHit(user);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      boolean isUser = entity.uuid().equals(user.uuid());
      double factor = isUser ? userConfig.powerSelf : userConfig.powerOther;
      BendingEffect.FIRE_TICK.reset(entity);
      if (factor == 0) {
        return false;
      }

      Vector3d push = ray.direction().normalize();
      if (!isUser) {
        // Cap vertical push
        push = push.withY(FastMath.clamp(push.y(), -0.3, 0.3));
      }

      factor *= 1 - (distanceTravelled / (2 * maxRange));
      // Reduce the push if the player is on the ground.
      if (isUser && user.isOnGround()) {
        factor *= 0.5;
      }
      Vector3d velocity = entity.velocity();
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
      entity.applyVelocity(AirBlast.this, velocity);
      entity.setProperty(EntityProperties.FALL_DISTANCE, 0F);
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
  private static final class Config implements Configurable {
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
    public List<String> path() {
      return List.of("abilities", "air", "airblast");
    }
  }
}
