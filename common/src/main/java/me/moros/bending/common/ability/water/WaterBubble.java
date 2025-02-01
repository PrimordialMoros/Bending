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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.functional.ExpireRemovalPolicy;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.functional.SwappedSlotsRemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Position;

public class WaterBubble extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Collection<Block> bubble = new HashSet<>();
  private Position center;

  private double radius = 1.5;
  private long nextUpdateTime = 0;

  public WaterBubble(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (user.game().abilityManager(user.worldKey()).hasAbility(user, WaterBubble.class)) {
      return false;
    }
    this.user = user;
    loadConfig();

    center = user.location().toVector3i();
    removalPolicy = Policies.builder()
      .add(Policies.NOT_SNEAKING)
      .add(SwappedSlotsRemovalPolicy.of(description()))
      .add(ExpireRemovalPolicy.of(userConfig.duration))
      .build();

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

    Position current = user.location().toVector3i();
    if (!current.equals(center)) {
      center = current;
      updateBubble = true;
    }

    if (updateBubble) {
      pushWater();
    }

    return UpdateResult.CONTINUE;
  }

  private boolean checkBlockOutOfRange(Block block) {
    if (block.distanceSq(user.location()) > radius * radius) {
      fastClean(block);
      return true;
    }
    return false;
  }

  private void pushWater() {
    bubble.removeIf(this::checkBlockOutOfRange);
    for (Block block : user.world().nearbyBlocks(user.location(), radius, MaterialUtil::isWater)) {
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
    for (Block block : bubble) {
      if (!MaterialUtil.isAir(block)) {
        continue;
      }
      TempBlock tb = TempBlock.MANAGER.get(block).orElse(null);
      if (tb == null) {
        continue;
      }
      double distance = block.distanceSq(center);
      double factor = distance > radius ? 0.3 : 1 - (distance / (1.5 * radius));
      long delay = (long) (1500 * factor);
      tb.revert();
      TempBlock.air().duration(delay).build(block);
    }
    user.addCooldown(description(), userConfig.cooldown);
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 3000;
    @Modifiable(Attribute.DURATION)
    private long duration = 15000;
    @Modifiable(Attribute.RADIUS)
    private double radius = 5;
    @Modifiable(Attribute.SPEED)
    private double speed = 0.5;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "waterbubble");
    }
  }
}
