/*
 * Copyright 2020-2025 Moros
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

package me.moros.bending.common.ability.water;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.collision.CollisionUtil;
import me.moros.bending.api.collision.geometry.Sphere;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.entity.Entity;
import me.moros.bending.api.platform.entity.EntityProperties;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempBlock.Builder;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.BendingEffect;
import me.moros.bending.api.util.Tasker;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Vector3d;

public class WaterWave extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Set<UUID> affectedEntities = new HashSet<>();

  private boolean ice = false;
  private long startTime;

  public WaterWave(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.PASSIVE) {
      return false;
    }
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, WaterWave.class)) {
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
    userConfig = user.game().configProcessor().calculate(this, Config.class);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    if (!user.canBuild()) {
      return UpdateResult.REMOVE;
    }

    // scale down to 0 speed near the end
    double factor = 1 - ((System.currentTimeMillis() - startTime) / (double) userConfig.duration);
    user.applyVelocity(this, user.direction().multiply(userConfig.speed * factor));
    user.setProperty(EntityProperties.FALL_DISTANCE, 0F);

    Vector3d center = user.location().add(Vector3d.MINUS_J);
    Collection<TempBlock> toRevert = new ArrayList<>();
    for (Block block : user.world().nearbyBlocks(center, userConfig.radius, MaterialUtil::isTransparent)) {
      if (TempBlock.MANAGER.isTemp(block)) {
        continue;
      }
      if (!user.canBuild(block)) {
        continue;
      }
      TempBlock.water().duration(1500).build(block).ifPresent(toRevert::add);
    }
    scheduleRevert(toRevert);
    if (ice) {
      CollisionUtil.handle(user, Sphere.of(center, userConfig.radius), this::onEntityHit);
    }
    return UpdateResult.CONTINUE;
  }

  private void scheduleRevert(Iterable<TempBlock> tempBlocks) {
    Tasker.sync().submit(() -> {
      final Consumer<TempBlock> consumer;
      if (ice) {
        Builder builder = TempBlock.ice().bendable(false).duration(1000);
        consumer = tb -> builder.build(tb.block());
      } else {
        consumer = TempBlock::revert;
      }
      tempBlocks.forEach(consumer);
    }, 20);
  }

  private boolean onEntityHit(Entity entity) {
    if (affectedEntities.add(entity.uuid())) {
      BendingEffect.FROST_TICK.apply(user, entity, userConfig.freezeTicks);
      entity.damage(userConfig.iceDamage, user, description());
      return true;
    }
    return false;
  }

  public void freeze() {
    ice = true;
  }

  public static void freeze(User user) {
    if (user.hasAbilitySelected("phasechange")) {
      user.game().abilityManager(user.worldKey()).firstInstance(user, WaterWave.class).ifPresent(WaterWave::freeze);
    }
  }

  @Override
  public void onDestroy() {
    user.addCooldown(description(), userConfig.cooldown);
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 6000;
    @Modifiable(Attribute.DURATION)
    private long duration = 3500;
    @Modifiable(Attribute.SPEED)
    private double speed = 1.2;
    @Modifiable(Attribute.RADIUS)
    private double radius = 1.7;
    @Modifiable(Attribute.DAMAGE)
    private double iceDamage = 2;
    @Modifiable(Attribute.FREEZE_TICKS)
    private int freezeTicks = 100;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "waterring", "waterwave");
    }
  }
}
