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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.Ability;
import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.MultiUpdatable;
import me.moros.bending.api.ability.common.basic.ParticleStream;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.particle.ParticleBuilder;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.common.ability.AbilityInitializer;
import me.moros.bending.common.config.ConfigManager;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class AirSwipe extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Set<Entity> affectedEntities = new HashSet<>();
  private final MultiUpdatable<AirStream> streams = MultiUpdatable.empty();

  private boolean charging;
  private double factor = 1;
  private long startTime;

  public AirSwipe(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();
    startTime = System.currentTimeMillis();
    charging = true;

    if (user.world().blockAt(user.mainHandSide()).type().isLiquid()) {
      return false;
    }

    for (AirSwipe swipe : user.game().abilityManager(user.worldKey()).userInstances(user, AirSwipe.class).toList()) {
      if (swipe.charging) {
        swipe.launch();
        return false;
      }
    }
    if (method == Activation.ATTACK) {
      launch();
    }
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(Policies.UNDER_WATER)
      .add(Policies.UNDER_LAVA)
      .build();

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
    if (charging) {
      if (user.sneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
        ParticleBuilder.air(user.mainHandSide()).spawn(user.world());
      } else if (!user.sneaking()) {
        launch();
      }
      return UpdateResult.CONTINUE;
    }
    return streams.update();
  }

  private void launch() {
    long deltaTime = System.currentTimeMillis() - startTime;
    factor = 1;
    if (deltaTime >= userConfig.maxChargeTime) {
      factor = userConfig.chargeFactor;
    } else if (deltaTime > 0.3 * userConfig.maxChargeTime) {
      double deltaFactor = (userConfig.chargeFactor - factor) * deltaTime / userConfig.maxChargeTime;
      factor += deltaFactor;
    }
    charging = false;
    user.addCooldown(description(), userConfig.cooldown);
    Vector3d origin = user.mainHandSide();
    Vector3d dir = user.direction();
    Vector3d rotateAxis = dir.cross(Vector3d.PLUS_J).normalize().cross(dir);
    int steps = userConfig.arc / 5;
    VectorUtil.createArc(dir, rotateAxis, Math.PI / 36, steps).forEach(
      v -> streams.add(new AirStream(Ray.of(origin, v.multiply(userConfig.range * factor))))
    );
    removalPolicy = Policies.builder().build();
  }

  @Override
  public void onCollision(Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (factor == userConfig.chargeFactor && collision.removeSelf()) {
      if (AbilityInitializer.layer2.containsValue(collidedAbility.description().key())) {
        collision.removeOther(true);
      } else {
        collision.removeSelf(false);
      }
    }
    if (collidedAbility instanceof AirSwipe other) {
      if (factor > other.factor + 0.1) {
        collision.removeSelf(false);
      }
    }
  }

  @Override
  public Collection<Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).toList();
  }

  private class AirStream extends ParticleStream {
    public AirStream(Ray ray) {
      super(user, ray, userConfig.speed, 0.5);
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b) || MaterialUtil.BREAKABLE_PLANTS.isTagged(b);
      livingOnly = false;
    }

    @Override
    public void render() {
      ParticleBuilder.air(location).spawn(user.world());
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundEffect.AIR.play(user.world(), location);
      }
    }

    @Override
    public boolean onEntityHit(Entity entity) {
      if (!affectedEntities.contains(entity)) {
        entity.damage(userConfig.damage * factor, user, description());
        Vector3d velocity = entity.center().subtract(ray.position()).normalize().multiply(factor);
        entity.applyVelocity(AirSwipe.this, velocity);
        affectedEntities.add(entity);
        return true;
      }
      return false;
    }

    @Override
    public boolean onBlockHit(Block block) {
      if (WorldUtil.tryBreakPlant(block) || WorldUtil.tryExtinguishFire(user, block)) {
        return false;
      }
      WorldUtil.tryCoolLava(user, block);
      return true;
    }
  }

  @ConfigSerializable
  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 1500;
    @Modifiable(Attribute.DAMAGE)
    private double damage = 2;
    @Modifiable(Attribute.RANGE)
    private double range = 9;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.8;
    private int arc = 35;
    @Comment("How many milliseconds it takes to fully charge")
    @Modifiable(Attribute.CHARGE_TIME)
    private long maxChargeTime = 2000;
    @Comment("How much the damage, range and knockback are multiplied by at full charge")
    @Modifiable(Attribute.STRENGTH)
    private double chargeFactor = 2;

    @Override
    public List<String> path() {
      return List.of("abilities", "air", "airswipe");
    }
  }
}
