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

package me.moros.bending.ability.water;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.ExpireRemovalPolicy;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.predicate.removal.SwappedSlotsRemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class WaterBubble extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Block> bubble = new HashSet<>();
  private Block center;

  private double radius = 1.5;
  private long nextUpdateTime = 0;

  public WaterBubble(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.world()).hasAbility(user, WaterBubble.class)) {
      return false;
    }
    this.user = user;
    loadConfig();

    center = user.locBlock();
    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();

    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = ConfigManager.calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }

    long time = System.currentTimeMillis();
    if (time < nextUpdateTime) {
      return UpdateResult.CONTINUE;
    }
    nextUpdateTime = time + 250;

    boolean updateBubble = false;
    if (radius < userConfig.radius) {
      radius += userConfig.speed;
      updateBubble = true;
    }

    Block currentBlock = user.locBlock();
    if (!currentBlock.equals(center)) {
      center = currentBlock;
      updateBubble = true;
    }

    if (updateBubble) {
      pushWater();
    }

    return UpdateResult.CONTINUE;
  }

  private boolean checkBlockOutOfRange(Block block) {
    if (block.getLocation().distanceSquared(user.entity().getLocation()) > radius * radius) {
      fastClean(block);
      return true;
    }
    return false;
  }

  private void pushWater() {
    bubble.removeIf(this::checkBlockOutOfRange);
    for (Block block : WorldUtil.nearbyBlocks(user.world(), user.location(), radius, MaterialUtil::isWater)) {
      if (!user.canBuild(block)) {
        continue;
      }
      if (TempBlock.MANAGER.isTemp(block)) {
        continue;
      }
      TempBlock.air().fixWater(false).build(block).ifPresent(tb -> bubble.add(block));
    }
  }

  private void fastClean(Block block) {
    TempBlock.MANAGER.get(block).filter(tb -> MaterialUtil.isAir(tb.block())).ifPresent(TempBlock::revert);
  }

  @Override
  public void onDestroy() {
    Vector3d centerLoc = new Vector3d(center);
    for (Block block : bubble) {
      if (!MaterialUtil.isAir(block)) {
        continue;
      }
      TempBlock tb = TempBlock.MANAGER.get(block).orElse(null);
      if (tb == null) {
        continue;
      }
      double distance = new Vector3d(block).distanceSq(centerLoc);
      double factor = distance > radius ? 0.3 : 1 - (distance / (1.5 * radius));
      long delay = (long) (1500 * factor);
      tb.revert();
      TempBlock.air().duration(delay).build(block);
    }
    user.addCooldown(description(), userConfig.cooldown);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 3000;
    @Modifiable(Attribute.DURATION)
    private long duration = 15000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 5;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.5;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "waterbubble");
    }
  }
}
