/*
 * Copyright 2020-2021 Moros
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

import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.PhaseTransformer;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.Ability;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.configurate.CommentedConfigurationNode;

public class PhaseChange extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Freeze freeze = new Freeze();
  private final Melt melt = new Melt();

  public PhaseChange(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().build();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
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
    if (!user.canBend(description()) || user.onCooldown(description())) {
      return;
    }
    Location center = RayTrace.of(user).range(userConfig.freezeRange).ignoreLiquids(false)
      .result(user.world()).position().toLocation(user.world());
    if (freeze.fillQueue(getShuffledBlocks(center, userConfig.freezeRadius, MaterialUtil::isWater))) {
      user.addCooldown(description(), userConfig.freezeCooldown);
    }
  }

  public void melt() {
    if (!user.canBend(description()) || user.onCooldown(description())) {
      return;
    }
    Location center = user.compositeRayTrace(userConfig.meltRange)
      .result(user.world()).position().toLocation(user.world());
    if (melt.fillQueue(getShuffledBlocks(center, userConfig.meltRadius, MaterialUtil::isMeltable))) {
      user.addCooldown(description(), 500);
    }
  }

  private Collection<Block> getShuffledBlocks(Location center, double radius, Predicate<Block> predicate) {
    List<Block> newBlocks = WorldUtil.nearbyBlocks(center, radius, predicate);
    newBlocks.removeIf(b -> !user.canBuild(b));
    Collections.shuffle(newBlocks);
    return newBlocks;
  }

  public static void freeze(@NonNull User user) {
    if (user.selectedAbilityName().equals("PhaseChange")) {
      Bending.game().abilityManager(user.world()).firstInstance(user, PhaseChange.class).ifPresent(PhaseChange::freeze);
    }
  }

  public static void melt(@NonNull User user) {
    if (user.selectedAbilityName().equals("PhaseChange")) {
      Bending.game().abilityManager(user.world()).firstInstance(user, PhaseChange.class).ifPresent(PhaseChange::melt);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class Freeze extends PhaseTransformer {
    @Override
    protected boolean processBlock(@NonNull Block block) {
      if (!MaterialUtil.isWater(block) || !TempBlock.isBendable(block)) {
        return false;
      }
      if (!user.canBuild(block)) {
        return false;
      }
      TempBlock.ice().build(block);
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.ICE.play(block.getLocation());
      }
      return true;
    }
  }

  private class Melt extends PhaseTransformer {
    @Override
    protected boolean processBlock(@NonNull Block block) {
      if (!TempBlock.isBendable(block)) {
        return false;
      }
      return WorldUtil.tryMelt(user, block);
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.SELECTION)
    public double freezeRange;
    @Modifiable(Attribute.RADIUS)
    public double freezeRadius;
    @Modifiable(Attribute.SPEED)
    public int freezeSpeed;
    @Modifiable(Attribute.COOLDOWN)
    public long freezeCooldown;

    @Modifiable(Attribute.SELECTION)
    public double meltRange;
    @Modifiable(Attribute.RADIUS)
    public double meltRadius;
    @Modifiable(Attribute.SPEED)
    public int meltSpeed;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "phasechange");

      freezeRange = abilityNode.node("freeze").node("range").getDouble(7.0);
      freezeRadius = abilityNode.node("freeze").node("radius").getDouble(3.5);
      freezeSpeed = abilityNode.node("freeze").node("speed").getInt(8);
      freezeCooldown = abilityNode.node("freeze").node("cooldown").getLong(2000);

      meltRange = abilityNode.node("melt").node("range").getDouble(7.0);
      meltRadius = abilityNode.node("melt").node("radius").getDouble(4.5);
      meltSpeed = abilityNode.node("melt").node("speed").getInt(8);

      abilityNode.node("freeze", "speed").comment("How many blocks can be affected per tick.");
      abilityNode.node("melt", "speed").comment("How many blocks can be affected per tick.");
    }
  }
}

