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

package me.moros.bending.ability.water.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.ability.common.SelectedSource;
import me.moros.bending.ability.water.IceCrawl;
import me.moros.bending.config.ConfigManager;
import me.moros.bending.config.Configurable;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.AbilityInstance;
import me.moros.bending.model.ability.Activation;
import me.moros.bending.model.ability.description.AbilityDescription;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.attribute.Attribute;
import me.moros.bending.model.attribute.Modifiable;
import me.moros.bending.model.math.FastMath;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.predicate.removal.Policies;
import me.moros.bending.model.predicate.removal.RemovalPolicy;
import me.moros.bending.model.properties.BendingProperties;
import me.moros.bending.model.user.User;
import me.moros.bending.util.WorldUtil;
import me.moros.bending.util.material.MaterialUtil;
import me.moros.bending.util.material.WaterMaterials;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

public class Iceberg extends AbilityInstance {
  private static final Config config = ConfigManager.load(Config::new);

  private User user;
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Vector3d tip;

  private final List<BlockIterator> lines = new ArrayList<>();
  private final Collection<Block> blocks = new HashSet<>();

  private boolean started = false;

  public Iceberg(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    if (user.game().abilityManager(user.world()).hasAbility(user, IceCrawl.class)) {
      return false;
    }

    Block source = user.find(userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
    if (source == null) {
      return false;
    }

    states = new StateChain()
      .addState(new SelectedSource(user, source, userConfig.selectRange + 2))
      .start();

    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
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
    if (!user.canBuild(block)) {
      return;
    }
    blocks.add(block);
    boolean canPlaceAir = !MaterialUtil.isWater(block) && !MaterialUtil.isAir(block);
    if (canPlaceAir) {
      TempBlock.air().duration(BendingProperties.instance().iceRevertTime() + userConfig.regenDelay).build(block);
    }
    Material ice = ThreadLocalRandom.current().nextBoolean() ? Material.PACKED_ICE : Material.ICE;
    TempBlock.builder(ice.createBlockData()).bendable(true).duration(BendingProperties.instance().iceRevertTime()).build(block);
  }

  public static void launch(User user) {
    if (user.selectedAbilityName().equals("IceSpike")) {
      user.game().abilityManager(user.world()).firstInstance(user, Iceberg.class).ifPresent(Iceberg::launch);
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
      Vector3d origin = Vector3d.center(src.get());
      Vector3d target = user.rayTrace(userConfig.selectRange + userConfig.length).entities(user.world())
        .entityCenterOrPosition();
      Vector3d direction = target.subtract(origin).normalize();
      tip = origin.add(direction.multiply(userConfig.length));
      Vector3d targetLocation = origin.add(direction.multiply(userConfig.length - 1)).snapToBlockCenter();
      double radius = FastMath.ceil(0.2 * userConfig.length);
      for (Block block : WorldUtil.nearbyBlocks(user.world(), origin, radius, WaterMaterials::isWaterOrIceBendable)) {
        if (!user.canBuild(block)) {
          continue;
        }
        lines.add(line(Vector3d.center(block), targetLocation));
      }
      if (lines.size() < 5) {
        lines.clear();
        return;
      }
      started = true;
    }
  }

  private BlockIterator line(Vector3d origin, Vector3d target) {
    Vector3d direction = target.subtract(origin);
    final int length = FastMath.round(target.distance(origin));
    return new BlockIterator(user.world(), origin.toBukkitVector(), direction.toBukkitVector(), 0, length);
  }

  @Override
  public void onDestroy() {
    if (!blocks.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  @Override
  public @MonotonicNonNull User user() {
    return user;
  }

  @ConfigSerializable
  private static class Config extends Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 15000;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 16;
    @Modifiable(Attribute.DURATION)
    private long regenDelay = 30000;
    @Modifiable(Attribute.HEIGHT)
    private double length = 16;

    @Override
    public Iterable<String> path() {
      return List.of("abilities", "water", "sequences", "iceberg");
    }
  }
}
