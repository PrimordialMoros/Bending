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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.model.ability.AbilityDescription;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.common.basic.PhaseTransformer;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.Policies;
import me.moros.bending.model.predicate.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Comment;

public class PhaseChange extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Freeze freeze = new Freeze();
  private final Melt melt = new Melt();

  public PhaseChange(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    if (method == Activation.ATTACK) {
      user.game().abilityManager(user.world()).firstInstance(user, PhaseChange.class).ifPresent(PhaseChange::freeze);
      return false;
    } else if (method == Activation.SNEAK) {
      user.game().abilityManager(user.world()).firstInstance(user, PhaseChange.class).ifPresent(PhaseChange::melt);
      return false;
    }
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = user.game().configProcessor().calculate(this, config);
  }

  @Override
  public UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description())) {
      freeze.clear();
      melt.clear();
      return UpdateResult.CONTINUE;
    }
    freeze.processQueue(userConfig.freezeSpeed);
    if (user.sneaking() && description().equals(user.selectedAbility())) {
      melt.processQueue(userConfig.meltSpeed);
    } else {
      melt.clear();
    }
    return UpdateResult.CONTINUE;
  }

  public void freeze() {
    if (user.onCooldown(description())) {
      return;
    }
    Vector3d center = user.rayTrace(userConfig.freezeRange).ignoreLiquids(false).blocks(user.world()).position();
    if (freeze.fillQueue(getShuffledBlocks(user.world(), center, userConfig.freezeRadius, MaterialUtil::isWater))) {
      user.addCooldown(description(), userConfig.freezeCooldown);
    }
  }

  public void melt() {
    if (user.onCooldown(description())) {
      return;
    }
    Vector3d center = user.rayTrace(userConfig.meltRange).blocks(user.world()).position();
    if (melt.fillQueue(getShuffledBlocks(user.world(), center, userConfig.meltRadius, MaterialUtil::isMeltable))) {
      user.addCooldown(description(), 500);
    }
  }

  private Collection<Block> getShuffledBlocks(World world, Vector3d center, double radius, Predicate<Block> predicate) {
    List<Block> newBlocks = WorldUtil.nearbyBlocks(world, center, radius, predicate);
    newBlocks.removeIf(b -> !user.canBuild(b));
    Collections.shuffle(newBlocks);
    return newBlocks;
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class Freeze extends PhaseTransformer {
    @Override
    protected boolean processBlock(Block block) {
      if (!MaterialUtil.isWater(block) || !TempBlock.isBendable(block)) {
        return false;
      }
      if (!user.canBuild(block)) {
        return false;
      }
      TempBlock.ice().build(block);
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.ICE.play(block);
      }
      return true;
    }
  }

  private class Melt extends PhaseTransformer {
    @Override
    protected boolean processBlock(Block block) {
      if (!TempBlock.isBendable(block)) {
        return false;
      }
      return WorldUtil.tryMelt(user, block);
    }
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.SELECTION)
    private double freezeRange = 7;
    @Modifiable(Attribute.RADIUS)
    private double freezeRadius = 3.5;
    @Comment("How many blocks can be affected per tick")
    @Modifiable(Attribute.SPEED)
    private int freezeSpeed = 8;
    @Modifiable(Attribute.COOLDOWN)
    private long freezeCooldown = 2000;
    @Modifiable(Attribute.SELECTION)
    private double meltRange = 7;
    @Modifiable(Attribute.RADIUS)
    private double meltRadius = 4.5;
    @Comment("How many blocks can be affected per tick")
    @Modifiable(Attribute.SPEED)
    private int meltSpeed = 8;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "phasechange");
    }
  }
}

