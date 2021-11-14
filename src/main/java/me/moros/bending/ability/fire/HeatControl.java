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

package me.moros.bending.ability.fire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import me.moros.atlas.configurate.CommentedConfigurationNode;
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
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.RayTrace;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.methods.BlockMethods;
import me.moros.bending.util.methods.WorldMethods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;

public class HeatControl extends AbilityInstance implements Ability {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private final Solidify solidify = new Solidify();
  private final Melt melt = new Melt();

  private long startTime;

  public HeatControl(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull Activation method) {
    this.user = user;
    loadConfig();
    removalPolicy = Policies.builder().build();
    startTime = System.currentTimeMillis();
    return true;
  }

  @Override
  public void loadConfig() {
    userConfig = Bending.configManager().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description()) || !user.canBend(description())) {
      solidify.clear();
      melt.clear();
      return UpdateResult.CONTINUE;
    }
    melt.processQueue(1);
    if (description().equals(user.selectedAbility())) {
      long time = System.currentTimeMillis();
      if (user.sneaking()) {
        if (isHoldingFood()) {
          ParticleUtil.createFire(user, user.mainHandSide().toLocation(user.world())).spawn();
          if (time > startTime + userConfig.cookInterval && cook()) {
            startTime = System.currentTimeMillis();
          }
          return UpdateResult.CONTINUE;
        }
        int freezeTicks = user.entity().getFreezeTicks();
        if (freezeTicks > 1) {
          user.entity().setFreezeTicks(freezeTicks - 2);
        }
        solidify.processQueue(1);
      } else {
        solidify.clear();
      }
      startTime = time;
    }
    return UpdateResult.CONTINUE;
  }

  private boolean isHoldingFood() {
    if (user instanceof BendingPlayer bendingPlayer) {
      return MaterialUtil.COOKABLE.containsKey(bendingPlayer.inventory().getItemInMainHand().getType());
    }
    return false;
  }

  private boolean cook() {
    if (user instanceof BendingPlayer bendingPlayer) {
      PlayerInventory inventory = bendingPlayer.inventory();
      Material cooked = MaterialUtil.COOKABLE.get(inventory.getItemInMainHand().getType());
      if (cooked != null) {
        inventory.addItem(new ItemStack(cooked)).values().forEach(item -> user.world().dropItem(user.headBlock().getLocation(), item));
        int amount = inventory.getItemInMainHand().getAmount();
        if (amount == 1) {
          inventory.clear(inventory.getHeldItemSlot());
        } else {
          inventory.getItemInMainHand().setAmount(amount - 1);
        }
        return true;
      }
    }
    return false;
  }

  private void act() {
    if (!user.canBend(description()) || user.onCooldown(description())) {
      return;
    }
    boolean acted = false;
    Location center = user.compositeRayTrace(userConfig.range)
      .result(user.world()).position().toLocation(user.world());
    Predicate<Block> predicate = b -> MaterialUtil.isFire(b) || MaterialUtil.isCampfire(b) || MaterialUtil.isMeltable(b);
    Predicate<Block> safe = b -> TempBlock.isBendable(b) && user.canBuild(b);
    List<Block> toMelt = new ArrayList<>();
    for (Block block : WorldMethods.nearbyBlocks(center, userConfig.radius, predicate.and(safe))) {
      acted = true;
      if (MaterialUtil.isFire(block) || MaterialUtil.isCampfire(block)) {
        BlockMethods.tryExtinguishFire(user, block);
      } else if (MaterialUtil.isMeltable(block)) {
        toMelt.add(block);
      }
    }
    if (!toMelt.isEmpty()) {
      Collections.shuffle(toMelt);
      melt.fillQueue(toMelt);
    }
    if (acted) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  private void onSneak() {
    if (!user.canBend(description()) || user.onCooldown(description())) {
      return;
    }
    Location center = RayTrace.of(user).range(userConfig.solidifyRange)
      .ignoreLiquids(false).result(user.world()).position().toLocation(user.world());
    if (solidify.fillQueue(getShuffledBlocks(center, userConfig.solidifyRadius, MaterialUtil::isLava))) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  public static void act(@NonNull User user) {
    if (user.selectedAbilityName().equals("HeatControl")) {
      Bending.game().abilityManager(user.world()).firstInstance(user, HeatControl.class).ifPresent(HeatControl::act);
    }
  }

  public static void onSneak(@NonNull User user) {
    if (user.selectedAbilityName().equals("HeatControl")) {
      Bending.game().abilityManager(user.world()).firstInstance(user, HeatControl.class).ifPresent(HeatControl::onSneak);
    }
  }

  private Collection<Block> getShuffledBlocks(Location center, double radius, Predicate<Block> predicate) {
    List<Block> newBlocks = WorldMethods.nearbyBlocks(center, radius, predicate);
    newBlocks.removeIf(b -> !user.canBuild(b));
    Collections.shuffle(newBlocks);
    return newBlocks;
  }

  public static boolean canBurn(@NonNull User user) {
    AbilityDescription selected = user.selectedAbility();
    if (selected == null) {
      return true;
    }
    return !selected.name().equals("HeatControl") || !user.canBend(selected);
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  private class Solidify extends PhaseTransformer {
    @Override
    protected boolean processBlock(@NonNull Block block) {
      if (MaterialUtil.isLava(block) && TempBlock.isBendable(block)) {
        return BlockMethods.tryCoolLava(user, block);
      }
      return false;
    }
  }

  private class Melt extends PhaseTransformer {
    @Override
    protected boolean processBlock(@NonNull Block block) {
      if (!TempBlock.isBendable(block)) {
        return false;
      }
      return BlockMethods.tryMelt(user, block);
    }
  }

  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    public long cooldown;
    @Modifiable(Attribute.RANGE)
    public double range;
    @Modifiable(Attribute.RADIUS)
    public double radius;
    @Modifiable(Attribute.RANGE)
    public double solidifyRange;
    @Modifiable(Attribute.RADIUS)
    public double solidifyRadius;
    @Modifiable(Attribute.CHARGE_TIME)
    public long cookInterval;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "fire", "heatcontrol");

      cooldown = abilityNode.node("cooldown").getLong(2000);
      range = abilityNode.node("range").getDouble(10.0);
      radius = abilityNode.node("radius").getDouble(5.0);
      solidifyRange = abilityNode.node("solidify-range").getDouble(5.0);
      solidifyRadius = abilityNode.node("solidify-radius").getDouble(6.0);
      cookInterval = abilityNode.node("cook-interval").getLong(2000);
    }
  }
}

