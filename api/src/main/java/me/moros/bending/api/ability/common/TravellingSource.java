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

package me.moros.bending.api.ability.common;

import me.moros.bending.api.ability.state.State;
import me.moros.bending.api.ability.state.StateChain;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Vector3d;
import org.jspecify.annotations.Nullable;

/**
 * State implementation for a source block that travels towards the user.
 */
public class TravellingSource implements State {
  private final BlockState state;
  private StateChain chain;
  private final User user;
  private Block source;

  private boolean started = false;

  private final double minDistanceSq, maxDistanceSq;

  public TravellingSource(User user, BlockState state, double minDistance, double maxDistance) {
    this.user = user;
    this.state = state;
    this.minDistanceSq = minDistance * minDistance;
    this.maxDistanceSq = maxDistance * maxDistance;
  }

  @Override
  public void start(StateChain chain) {
    if (started) {
      return;
    }
    this.chain = chain;
    source = chain.chainStore().stream().findFirst().orElse(null);
    started = source != null;
  }

  @Override
  public void complete() {
    if (!started) {
      return;
    }
    chain.chainStore().clear();
    chain.chainStore().add(source);
    chain.nextState();
  }

  @Override
  public UpdateResult update() {
    if (!started) {
      return UpdateResult.REMOVE;
    }
    clean();
    Vector3d target = user.location().center();
    Vector3d location = source.center();

    double distSq = target.distanceSq(location);
    if (maxDistanceSq > minDistanceSq && distSq > maxDistanceSq) {
      return UpdateResult.REMOVE;
    }
    if (target.distanceSq(location) < minDistanceSq) {
      complete();
      return UpdateResult.CONTINUE;
    }

    int y = user.eyeLocation().blockY();

    if (isValid(source.offset(Direction.UP)) && source.y() < y) {
      source = source.offset(Direction.UP);
    } else if (isValid(source.offset(Direction.DOWN)) && source.y() > y) {
      source = source.offset(Direction.DOWN);
    } else {
      Vector3d direction = target.subtract(location).normalize();
      Block nextBlock = user.world().blockAt(location.add(direction));
      if (source.equals(nextBlock)) {
        source = findPath(nextBlock);
      } else {
        source = nextBlock;
      }
    }
    if (source == null || !isValid(source) || !user.canBuild(source)) {
      return UpdateResult.REMOVE;
    }
    TempBlock.builder(state).duration(200).build(source);
    return UpdateResult.CONTINUE;
  }

  private @Nullable Block findPath(Block check) {
    Vector3d dest = user.eyeLocation().center();
    Block result = null;
    double minDistance = Double.MAX_VALUE;
    for (Direction face : WorldUtil.SIDES) {
      Block block = check.offset(face);
      if (!isValid(block)) {
        continue;
      }
      double d = block.center().distanceSq(dest);
      if (d < minDistance) {
        minDistance = d;
        result = block;
      }
    }
    return result;
  }

  private boolean isValid(Block block) {
    if (!TempBlock.isBendable(block)) {
      return false;
    }
    if (state.type() == BlockType.WATER) {
      return MaterialUtil.isTransparentOrWater(block);
    }
    return MaterialUtil.isTransparent(block);
  }

  private void clean() {
    if (state.type() == source.type()) {
      TempBlock.air().build(source);
    }
  }
}
