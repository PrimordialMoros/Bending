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

package me.moros.bending.ability.air;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.ParticleStream;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.AbilityInitializer;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.VectorMethods;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AirSwipe extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Set<Entity> affectedEntities = new HashSet<>();
  private final List<AirStream> streams = new ArrayList<>();

  private boolean charging;
  private double factor = 1;
  private long startTime;

  public AirSwipe(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    startTime = System.currentTimeMillis();
    charging = true;

    if (user.mainHandSide().toBlock(user.world()).isLiquid()) {
      return false;
    }

    for (AirSwipe swipe : Bending.game().abilityManager(user.world()).userInstances(user, AirSwipe.class).collect(Collectors.toList())) {
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
      .add(Policies.IN_LIQUID)
      .build();

    return true;
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
    if (charging) {
      if (user.sneaking() && System.currentTimeMillis() >= startTime + userConfig.maxChargeTime) {
        ParticleUtil.createAir(user.mainHandSide().toLocation(user.world())).spawn();
      } else if (!user.sneaking()) {
        launch();
      }
    } else {
      streams.removeIf(stream -> stream.update() == UpdateResult.REMOVE);
    }

    return (charging || !streams.isEmpty()) ? UpdateResult.CONTINUE : UpdateResult.REMOVE;
  }

  private void launch() {
    long deltaTime = System.currentTimeMillis() - startTime;
    factor = 1;
    if (deltaTime >= userConfig.maxChargeTime) {
      factor = userConfig.chargeFactor;
    } else if (deltaTime > 0.3 * userConfig.maxChargeTime) {
      double deltaFactor = (userConfig.chargeFactor - factor) * deltaTime / (double) userConfig.maxChargeTime;
      factor += deltaFactor;
    }
    charging = false;
    user.addCooldown(description(), userConfig.cooldown);
    Vector3d origin = user.mainHandSide();
    Vector3d dir = user.direction();
    Vector3d rotateAxis = dir.cross(Vector3d.PLUS_J).normalize().cross(dir);
    int steps = userConfig.arc / 5;
    VectorMethods.createArc(dir, rotateAxis, Math.PI / 36, steps).forEach(
      v -> streams.add(new AirStream(new Ray(origin, v.multiply(userConfig.range * factor))))
    );
    removalPolicy = Policies.builder().build();
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (factor == userConfig.chargeFactor && collision.removeSelf()) {
      String name = collidedAbility.description().name();
      if (AbilityInitializer.layer2.contains(name)) {
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
  public @NonNull Collection<@NonNull Collider> colliders() {
    return streams.stream().map(ParticleStream::collider).collect(Collectors.toList());
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class AirStream extends ParticleStream {
    public AirStream(Ray ray) {
      super(user, ray, userConfig.speed, 0.5);
      canCollide = b -> b.isLiquid() || MaterialUtil.isFire(b) || MaterialUtil.BREAKABLE_PLANTS.isTagged(b);
      livingOnly = false;
    }

    @Override
    public void render() {
      ParticleUtil.createAir(bukkitLocation()).spawn();
    }

    @Override
    public void postRender() {
      if (ThreadLocalRandom.current().nextInt(6) == 0) {
        SoundUtil.AIR.play(bukkitLocation());
      }
    }

    @Override
    public boolean onEntityHit(@NonNull Entity entity) {
      if (!affectedEntities.contains(entity)) {
        DamageUtil.damageEntity(entity, user, userConfig.damage * factor, description());
        Vector3d velocity = EntityMethods.entityCenter(entity).subtract(ray.origin).normalize().multiply(factor);
        EntityMethods.applyVelocity(AirSwipe.this, entity, velocity);
        affectedEntities.add(entity);
        return true;
      }
      return false;
    }

    @Override
    public boolean onBlockHit(@NonNull Block block) {
      if (BlockMethods.tryBreakPlant(block) || BlockMethods.tryExtinguishFire(user, block)) {
        return false;
      }
      BlockMethods.tryCoolLava(user, block);
      return true;
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.RANGE)
    public int range;
    @Modifiable(Attribute.SPEED)
    public double speed;
    public int arc;
    @Modifiable(Attribute.CHARGE_TIME)
    public long maxChargeTime;
    @Modifiable(Attribute.STRENGTH)
    public double chargeFactor;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airswipe");

      cooldown = abilityNode.node("cooldown").getLong(1500);
      damage = abilityNode.node("damage").getDouble(2.0);
      range = abilityNode.node("range").getInt(9);
      speed = abilityNode.node("speed").getDouble(0.8);
      arc = abilityNode.node("arc").getInt(35);

      chargeFactor = abilityNode.node("charge").node("factor").getDouble(2.0);
      maxChargeTime = abilityNode.node("charge").node("max-time").getLong(2000);

      abilityNode.node("arc").comment("How large the entire arc is in degrees");

      abilityNode.node("charge").node("factor").comment("How much the damage, range and knockback are multiplied by at full charge");
      abilityNode.node("charge").node("max-time").comment("How many milliseconds it takes to fully charge");
    }
  }
}
