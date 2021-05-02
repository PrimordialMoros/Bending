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

package me.moros.bending.ability.water.sequences;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.atlas.configurate.CommentedConfigurationNode;
import me.moros.bending.Bending;
import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.water.IceCrawl;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.ability.util.ActivationMethod;
import me.moros.bending.model.ability.util.UpdateResult;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.math.Vector3;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.user.User;
import me.moros.bending.util.BendingProperties;
import me.moros.bending.util.SourceUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import me.moros.bending.util.methods.WorldMethods;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.NumberConversions;
import org.checkerframework.checker.nullness.qual.NonNull;

public class Iceberg extends AbilityInstance {
  private static final Config config = new Config();

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Vector3 tip;

  private final List<BlockIterator> lines = new ArrayList<>();
  private final Collection<Block> blocks = new HashSet<>();

  private boolean started = false;

  public Iceberg(@NonNull AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(@NonNull User user, @NonNull ActivationMethod method) {
    this.user = user;
    recalculateConfig();

    if (Bending.game().abilityManager(user.world()).hasAbility(user, IceCrawl.class)) {
      return false;
    }

    Optional<Block> source = SourceUtil.find(user, userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
    if (source.isEmpty()) {
      return false;
    }

    states = new StateChain()
      .addState(new SelectedSource(user, source.get(), userConfig.selectRange + 2))
      .start();

    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
    return true;
  }

  @Override
  public void recalculateConfig() {
    userConfig = Bending.game().attributeSystem().calculate(this, config);
  }

  @Override
  public @NonNull UpdateResult update() {
    if (removalPolicy.test(user, description())) {
      return UpdateResult.REMOVE;
    }
    if (started) {
      ListIterator<BlockIterator> iterator = lines.listIterator();
      while (iterator.hasNext()) {
        if (ThreadLocalRandom.current().nextInt(1 + lines.size()) == 0) {
          continue;
        }
        BlockIterator blockLine = iterator.next();
        if (blockLine.hasNext()) {
          formIce(blockLine.next());
        } else {
          iterator.remove();
        }
      }
      if (lines.isEmpty()) {
        formIce(tip.toBlock(user.world()));
        return UpdateResult.REMOVE;
      }
      return UpdateResult.CONTINUE;
    } else {
      if (!user.selectedAbilityName().equals("IceSpike")) {
        return UpdateResult.REMOVE;
      }
      return states.update();
    }
  }

  private void formIce(Block block) {
    if (blocks.contains(block) || TempBlock.MANAGER.isTemp(block) || MaterialUtil.isUnbreakable(block)) {
      return;
    }
    if (!Bending.game().protectionSystem().canBuild(user, block)) {
      return;
    }
    blocks.add(block);
    boolean canPlaceAir = !MaterialUtil.isWater(block) && !MaterialUtil.isAir(block);
    if (canPlaceAir) {
      TempBlock.createAir(block, BendingProperties.ICE_DURATION + userConfig.regenDelay);
    }
    Material ice = ThreadLocalRandom.current().nextBoolean() ? Material.PACKED_ICE : Material.ICE;
    TempBlock.create(block, ice.createBlockData(), BendingProperties.ICE_DURATION, true);
  }

  public static void launch(User user) {
    if (user.selectedAbilityName().equals("IceSpike")) {
      Bending.game().abilityManager(user.world()).firstInstance(user, Iceberg.class).ifPresent(Iceberg::launch);
    }
  }

  private void launch() {
    if (started) {
      return;
    }
    State state = states.current();
    if (state instanceof SelectedSource) {
      state.complete();
      Optional<Block> src = states.chainStore().stream().findAny();
      if (src.isEmpty()) {
        return;
      }
      Vector3 origin = new Vector3(src.get()).add(Vector3.HALF);
      Vector3 target = user.rayTrace(userConfig.selectRange + userConfig.length);
      Vector3 direction = target.subtract(origin).normalize();
      tip = origin.add(direction.scalarMultiply(userConfig.length));
      Vector3 targetLocation = origin.add(direction.scalarMultiply(userConfig.length - 1)).floor().add(Vector3.HALF);
      double radius = FastMath.ceil(0.2 * userConfig.length);
      for (Block block : WorldMethods.nearbyBlocks(origin.toLocation(user.world()), radius, WaterMaterials::isWaterOrIceBendable)) {
        if (!Bending.game().protectionSystem().canBuild(user, block)) {
          continue;
        }
        lines.add(line(new Vector3(block).add(Vector3.HALF), targetLocation));
      }
      if (lines.size() < 5) {
        lines.clear();
        return;
      }
      started = true;
    }
  }

  private BlockIterator line(Vector3 origin, Vector3 target) {
    Vector3 direction = target.subtract(origin);
    final double length = target.distance(origin);
    return new BlockIterator(user.world(), origin.toVector(), direction.toVector(), 0, NumberConversions.round(length));
  }

  @Override
  public void onDestroy() {
    if (!blocks.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  @Override
  public @NonNull User user() {
    return user;
  }

  private static class Config extends Configurable {
    @Attribute(Attribute.COOLDOWN)
    public long cooldown;
    @Attribute(Attribute.SELECTION)
    public double selectRange;

    @Attribute(Attribute.DURATION)
    public long regenDelay;
    @Attribute(Attribute.HEIGHT)
    public double length;

    @Override
    public void onConfigReload() {
      CommentedConfigurationNode abilityNode = config.node("abilities", "water", "sequences", "iceberg");

      cooldown = abilityNode.node("cooldown").getLong(15000);
      selectRange = abilityNode.node("select-range").getDouble(16.0);
      regenDelay = abilityNode.node("regen-delay").getLong(30000);
      length = abilityNode.node("length").getDouble(16.0);
    }
  }
}
