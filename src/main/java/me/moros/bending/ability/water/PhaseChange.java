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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.atlas.cf.checker.nullness.qual.NonNull;
import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.basic.PhaseTransformer;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.PassiveAbility;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class PhaseChange extends AbilityInstance implements PassiveAbility {
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
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();
    removalPolicy = Policies.builder().build();
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.getGame().getAttributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, getDescription()) || !user.canBend(getDescription())) {
      freeze.clear();
      melt.clear();
      return UpdateResult.CONTINUE;
    }
    freeze.processQueue(userConfig.freezeSpeed);
    if (user.isSneaking() && getDescription().equals(user.getSelectedAbility().orElse(null))) {
      melt.processQueue(userConfig.meltSpeed);
    } else {
      melt.clear();
    }
    return UpdateResult.CONTINUE;
  }

  public void freeze() {
    if (!user.canBend(getDescription()) || user.isOnCooldown(getDescription())) {
      return;
    }
    Location center = user.getTarget(userConfig.freezeRange, false).toLocation(user.getWorld());
    if (freeze.fillQueue(getShuffledBlocks(center, userConfig.freezeRadius, MaterialUtil::isWater))) {
      user.setCooldown(getDescription(), userConfig.freezeCooldown);
    }
  }

  public void melt() {
    if (!user.canBend(getDescription()) || user.isOnCooldown(getDescription())) {
      return;
    }
    user.setCooldown(getDescription(), 500);
    Location center = user.getTarget(userConfig.meltRange).toLocation(user.getWorld());
    melt.fillQueue(getShuffledBlocks(center, userConfig.meltRadius, b -> MaterialUtil.isSnow(b) || WaterMaterials.isIceBendable(b)));
  }

  private Collection<Block> getShuffledBlocks(Location center, double radius, Predicate<Block> predicate) {
    List<Block> newBlocks = WorldMethods.getNearbyBlocks(center, radius, predicate);
    newBlocks.removeIf(b -> !Bending.getGame().getProtectionSystem().canBuild(user, b));
    Collections.shuffle(newBlocks);
    return newBlocks;
  }

  public static void freeze(User user) {
    if (user.getSelectedAbilityName().equals("PhaseChange")) {
      Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, PhaseChange.class).ifPresent(PhaseChange::freeze);
    }
  }

  public static void melt(User user) {
    if (user.getSelectedAbilityName().equals("PhaseChange")) {
      Bending.getGame().getAbilityManager(user.getWorld()).getFirstInstance(user, PhaseChange.class).ifPresent(PhaseChange::melt);
    }
  }

  @Override
  public @NonNull User getUser() {
    return user;
  }

  private class Freeze extends PhaseTransformer {
    @Override
    protected boolean processBlock(@NonNull Block block) {
      if (!MaterialUtil.isWater(block) || !TempBlock.isBendable(block)) {
        return false;
      }
      if (!Bending.getGame().getProtectionSystem().canBuild(user, block)) {
        return false;
      }

      TempBlock.create(block, Material.ICE.createBlockData(), true);
      if (ThreadLocalRandom.current().nextInt(12) == 0) {
        SoundUtil.ICE_SOUND.play(block.getLocation());
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
      return BlockMethods.tryMeltSnow(user, block) || BlockMethods.tryMeltIce(user, block);
    }
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.SELECTION)
    public double freezeRange;
    @Attribute(Attribute.RADIUS)
    public double freezeRadius;
    @Attribute(Attribute.SPEED)
    public int freezeSpeed;
    @Attribute(Attribute.COOLDOWN)
    public long freezeCooldown;

    @Attribute(Attribute.SELECTION)
    public double meltRange;
    @Attribute(Attribute.RADIUS)
    public double meltRadius;
    @Attribute(Attribute.SPEED)
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

