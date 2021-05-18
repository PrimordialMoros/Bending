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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.water.FrostBreath;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.collision.Collider;
import me.moros.bending.model.collision.Collision;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.EntityMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.NonNull;

public class AirShield extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private long currentPoint = 0;
  private long startTime;

  public AirShield(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();
    removalPolicy = Policies.builder()
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(Policies.NOT_SNEAKING)
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();
    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBuild(user.headBlock())) {
      return UpdateResult.REMOVE;
    }
    currentPoint++;
    Vector3 center = center();
    double spacing = userConfig.radius / 4;
    for (int i = 1; i < 8; i++) {
      double y = (i * spacing) - userConfig.radius;
      double factor = 1 - (y * y) / (userConfig.radius * userConfig.radius);
      if (factor <= 0.2) {
        continue;
      }
      double x = userConfig.radius * factor * Math.cos(i * currentPoint);
      double z = userConfig.radius * factor * Math.sin(i * currentPoint);
      Vector3 loc = center.add(new Vector3(x, y, z));
      ParticleUtil.createAir(loc.toLocation(user.world())).count(5)
        .offset(0.2, 0.2, 0.2).extra(0.01).spawn();
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.AIR_SOUND.play(loc.toLocation(user.world()));
      }
    }

    for (Block b : WorldMethods.nearbyBlocks(center.toLocation(user.world()), userConfig.radius, MaterialUtil::isFire)) {
      BlockMethods.tryCoolLava(user, b);
      BlockMethods.tryExtinguishFire(user, b);
    }

    CollisionUtil.handleEntityCollisions(user, new Sphere(center, userConfig.radius), entity -> {
      Vector3 toEntity = new Vector3(entity.getLocation()).subtract(center);
      Vector3 normal = toEntity.setY(0).normalize();
      double strength = ((userConfig.radius - toEntity.getNorm()) / userConfig.radius) * userConfig.maxPush;
      strength = Math.max(0, Math.min(1, strength));
      entity.setVelocity(entity.getVelocity().add(normal.multiply(strength).clampVelocity()));
      return false;
    }, false);

    return UpdateResult.CONTINUE;
  }

  private Vector3 center() {
    return EntityMethods.entityCenter(user.entity());
  }

  @Override
  public void onDestroy() {
    double factor = userConfig.duration == 0 ? 1 : System.currentTimeMillis() - startTime / (double) userConfig.duration;
    long cooldown = Math.min(1000, (long) (factor * userConfig.cooldown));
    user.addCooldown(description(), cooldown);
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  @Override
  public @NonNull Collection<@NonNull Collider> colliders() {
    return List.of(new Sphere(center(), userConfig.radius));
  }

  @Override
  public void onCollision(@NonNull Collision collision) {
    Ability collidedAbility = collision.collidedAbility();
    if (collidedAbility instanceof FrostBreath) {
      for (Block block : WorldMethods.nearbyBlocks(center().toLocation(user.world()), userConfig.radius, MaterialUtil::isTransparentOrWater)) {
        if (!user.canBuild(block)) {
          continue;
        }
        BlockMethods.tryBreakPlant(block);
        if (MaterialUtil.isAir(block) || MaterialUtil.isWater(block)) {
          long iceDuration = BendingProperties.ICE_DURATION + ThreadLocalRandom.current().nextInt(1500);
          TempBlock.create(block, Material.ICE.createBlockData(), iceDuration, true);
        }
      }
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.DURATION)
    public long duration;
    @Attribute(Attribute.RADIUS)
    public double radius;
    @Attribute(Attribute.STRENGTH)
    public double maxPush;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "air", "airshield");

      cooldown = abilityNode.node("cooldown").getLong(4000);
      duration = abilityNode.node("duration").getLong(10000);
      radius = abilityNode.node("radius").getDouble(4.0);
      maxPush = abilityNode.node("max-push").getDouble(2.6);
    }
  }
}
