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

package me.moros.bending.ability.water;

import java.util.HashSet;
import java.util.Set;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.collision.geometry.Sphere;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.DamageUtil;
import me.moros.bending.util.PotionUtil;
import me.moros.bending.util.Tasker;
import me.moros.bending.util.collision.CollisionUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class WaterWave extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Set<Entity> affectedEntities = new HashSet<>();

  private boolean ice = false;
  private long startTime;

  public WaterWave(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    if (Bending.game().abilityManager(user.world()).hasAbility(user, WaterWave.class)) {
      return false;
    }

    this.user = user;
    loadConfig();

    removalPolicy = Policies.builder().add(ExpireRemovalPolicy.of(userConfig.duration)).build();
    startTime = System.currentTimeMillis();
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

    if (!user.canBuild(user.locBlock())) {
      return UpdateResult.REMOVE;
    }

    // scale down to 0 speed near the end
    double factor = 1 - ((System.currentTimeMillis() - startTime) / (double) userConfig.duration);

    user.entity().setVelocity(user.direction().multiply(userConfig.speed * factor).clampVelocity());
    user.entity().setFallDistance(0);

    Vector3 center = user.location().add(Vector3.MINUS_J);
    for (Block block : WorldMethods.nearbyBlocks(center.toLocation(user.world()), userConfig.radius, MaterialUtil::isTransparent)) {
      if (TempBlock.MANAGER.isTemp(block)) {
        continue;
      }
      if (!user.canBuild(block)) {
        continue;
      }
      TempBlock.create(block, Material.WATER.createBlockData(), 1500).ifPresent(this::scheduleRevert);
    }
    if (ice) {
      CollisionUtil.handleEntityCollisions(user, new Sphere(center, userConfig.radius), this::onEntityHit);
    }
    return UpdateResult.CONTINUE;
  }

  private void scheduleRevert(TempBlock tb) {
    final Block block = tb.block();
    Tasker.sync(() -> {
      if (ice) {
        TempBlock.create(block, Material.ICE.createBlockData(), 1000);
      } else {
        tb.revert();
      }
    }, 20);
  }

  private boolean onEntityHit(Entity entity) {
    if (affectedEntities.contains(entity)) {
      return false;
    }
    affectedEntities.add(entity);
    DamageUtil.damageEntity(entity, user, userConfig.damage, description());
    int potionDuration = NumberConversions.round(userConfig.slowDuration / 50.0);
    PotionUtil.tryAddPotion(entity, PotionEffectType.SLOW, potionDuration, userConfig.power);
    return true;
  }

  public void freeze() {
    ice = true;
  }

  public static void freeze(@NonNull User user) {
    if (user.selectedAbilityName().equals("PhaseChange")) {
      Bending.game().abilityManager(user.world()).firstInstance(user, WaterWave.class).ifPresent(WaterWave::freeze);
    }
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.DURATION)
    public long duration;
    @Modifiable(Attribute.SPEED)
    public double speed;
    @Modifiable(Attribute.RADIUS)
    public double radius;

    @Modifiable(Attribute.DAMAGE)
    public double damage;
    @Modifiable(Attribute.STRENGTH)
    public int power;
    @Modifiable(Attribute.DURATION)
    public long slowDuration;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "waterring", "waterwave");

      cooldown = abilityNode.node("cooldown").getLong(6000);
      duration = abilityNode.node("duration").getLong(3500);
      speed = abilityNode.node("speed").getDouble(1.2);
      radius = abilityNode.node("radius").getDouble(1.7);

      damage = abilityNode.node("ice-damage").getDouble(2.0);
      power = abilityNode.node("ice-slow-power").getInt(2) - 1;
      slowDuration = abilityNode.node("ice-slow-duration").getLong(3500);
    }
  }
}
