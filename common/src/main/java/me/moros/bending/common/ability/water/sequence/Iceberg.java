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

package me.moros.bending.common.ability.water.sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.ability.common.SelectedSource;
import me.moros.bending.api.ability.state.State;
import me.moros.bending.api.ability.state.StateChain;
import me.moros.bending.api.config.BendingProperties;
import me.moros.bending.api.config.Configurable;
import me.moros.bending.api.config.attribute.Attribute;
import me.moros.bending.api.config.attribute.Modifiable;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.GridIterator;
import me.moros.bending.api.util.functional.Policies;
import me.moros.bending.api.util.functional.RemovalPolicy;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.bending.api.util.material.WaterMaterials;
import me.moros.bending.common.ability.water.IceCrawl;
import me.moros.math.FastMath;
import me.moros.math.Position;
import me.moros.math.Vector3d;

public class Iceberg extends AbilityInstance {
  private Config userConfig;
  private RemovalPolicy removalPolicy;

  private StateChain states;
  private Vector3d tip;

  private final List<GridIterator> lines = new ArrayList<>();
  private final Collection<Block> blocks = new HashSet<>();

  private boolean started = false;

  public Iceberg(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    this.user = user;
    loadConfig();

    if (user.game().abilityManager(user.worldKey()).hasAbility(user, IceCrawl.class)) {
      return false;
    }

    Block source = user.find(userConfig.selectRange, WaterMaterials::isWaterOrIceBendable);
    if (source == null) {
      return false;
    }

    states = new StateChain()
      .addState(SelectedSource.create(user, source, userConfig.selectRange + 2))
      .start();

    removalPolicy = Policies.builder().add(Policies.NOT_SNEAKING).build();
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
    if (started) {
      ListIterator<GridIterator> iterator = lines.listIterator();
      while (iterator.hasNext()) {
        if (ThreadLocalRandom.current().nextInt(1 + lines.size()) == 0) {
          continue;
        }
        GridIterator blockLine = iterator.next();
        if (blockLine.hasNext()) {
          formIce(blockLine.next());
        } else {
          iterator.remove();
        }
      }
      if (lines.isEmpty()) {
        formIce(tip);
        return UpdateResult.REMOVE;
      }
      return UpdateResult.CONTINUE;
    } else {
      if (!user.hasAbilitySelected("icespike")) {
        return UpdateResult.REMOVE;
      }
      return states.update();
    }
  }

  private void formIce(Position pos) {
    Block block = user.world().blockAt(pos);
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
    BlockType ice = ThreadLocalRandom.current().nextBoolean() ? BlockType.PACKED_ICE : BlockType.ICE;
    TempBlock.builder(ice).bendable(true).duration(BendingProperties.instance().iceRevertTime()).build(block);
  }

  public static void launch(User user) {
    if (user.hasAbilitySelected("icespike")) {
      user.game().abilityManager(user.worldKey()).firstInstance(user, Iceberg.class).ifPresent(Iceberg::launch);
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
      Vector3d origin = src.get().center();
      Vector3d target = user.rayTrace(userConfig.selectRange + userConfig.length).cast(user.world())
        .entityCenterOrPosition();
      Vector3d direction = target.subtract(origin).normalize();
      tip = origin.add(direction.multiply(userConfig.length));
      Vector3d targetLocation = origin.add(direction.multiply(userConfig.length - 1)).center();
      double radius = FastMath.ceil(0.2 * userConfig.length);
      for (Block block : user.world().nearbyBlocks(origin, radius, WaterMaterials::isWaterOrIceBendable)) {
        if (!user.canBuild(block)) {
          continue;
        }
        lines.add(line(block.center(), targetLocation));
      }
      if (lines.size() < 5) {
        lines.clear();
        return;
      }
      started = true;
    }
  }

  private GridIterator line(Vector3d origin, Vector3d target) {
    Vector3d direction = target.subtract(origin);
    final int length = FastMath.round(target.distance(origin));
    return GridIterator.create(origin, direction, length);
  }

  @Override
  public void onDestroy() {
    if (!blocks.isEmpty()) {
      user.addCooldown(description(), userConfig.cooldown);
    }
  }

  private static final class Config implements Configurable {
    @Modifiable(Attribute.COOLDOWN)
    private long cooldown = 15000;
    @Modifiable(Attribute.SELECTION)
    private double selectRange = 16;
    @Modifiable(Attribute.DURATION)
    private long regenDelay = 30000;
    @Modifiable(Attribute.HEIGHT)
    private double length = 16;

    @Override
    public List<String> path() {
      return List.of("abilities", "water", "sequences", "iceberg");
    }
  }
}
