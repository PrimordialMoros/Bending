/*
 * Copyright 2020-2023 Moros
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

package me.moros.bending.model.ability.common;

import java.util.HashMap;
import java.util.Map;

import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.particle.Particle;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempBlock.Snapshot;
import me.moros.math.Vector3d;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * State implementation for focusing a selected source.
 */
public class SelectedSource implements State {
  private static final Map<Block, SelectedSource> INSTANCES = new HashMap<>();

  private StateChain chain;
  private final User user;
  private Vector3d origin;
  private Block block;
  private BlockType type;
  private Snapshot snapshot;

  private final boolean particles;
  private final double distanceSq;

  private boolean started;
  private boolean forceRemove;

  public SelectedSource(User user, Block block, double maxDistance, @Nullable BlockState state) {
    this.user = user;
    this.distanceSq = 0.25 + maxDistance * maxDistance;
    particles = state == null;
    reselect(block, state);
  }

  public SelectedSource(User user, Block block, double maxDistance) {
    this(user, block, maxDistance, null);
  }

  public boolean reselect(Block block) {
    return reselect(block, null);
  }

  public boolean reselect(Block block, @Nullable BlockState state) {
    if (block.equals(this.block)) {
      return false;
    }
    Vector3d newOrigin = block.center();
    if (user.eyeLocation().distanceSq(newOrigin) > distanceSq) {
      return false;
    }
    onDestroy();
    this.block = block;
    this.origin = newOrigin;
    this.type = state == null ? block.type() : state.type();
    if (state != null) {
      snapshot = TempBlock.MANAGER.get(block).map(TempBlock::snapshot).orElse(null);
      TempBlock.builder(state).build(block);
      INSTANCES.put(block, this);
    }
    return true;
  }

  @Override
  public void start(StateChain chain) {
    if (started) {
      return;
    }
    this.chain = chain;
    started = origin != null;
  }

  @Override
  public void complete() {
    if (!started) {
      return;
    }
    if (block.type() != type) {
      forceRemove = true;
    }
    onDestroy();
    chain.chainStore().clear();
    if (forceRemove) {
      return;
    }
    chain.chainStore().add(block);
    chain.nextState();
  }

  @Override
  public UpdateResult update() {
    if (!started || forceRemove) {
      return UpdateResult.REMOVE;
    }
    if (user.eyeLocation().distanceSq(origin) > distanceSq) {
      return UpdateResult.REMOVE;
    }
    if (particles) {
      Particle.SMOKE.builder(origin.add(0, 0.5, 0)).spawn(user.world());
    }
    return UpdateResult.CONTINUE;
  }

  public Block selectedSource() {
    return block;
  }

  public void onDestroy() {
    if (!particles && block != null && block.type() == type) {
      TempBlock.revertToSnapshot(block, snapshot);
    }
    INSTANCES.remove(block);
  }

  public static void tryRevertSource(Block block) {
    SelectedSource selectedSource = INSTANCES.get(block);
    if (selectedSource != null) {
      selectedSource.onDestroy();
    }
  }
}
