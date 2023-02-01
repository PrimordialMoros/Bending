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

import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.user.User;
import me.moros.bending.platform.block.Block;
import me.moros.bending.platform.block.BlockType;
import me.moros.bending.platform.particle.Particle;
import me.moros.math.Vector3d;

class SelectedSourceImpl implements SelectedSource {
  private StateChain chain;
  private final User user;
  private Vector3d origin;
  protected Block block;
  protected BlockType type;

  private final double distanceSq;

  private boolean started;
  private boolean forceRemove;

  protected SelectedSourceImpl(User user, Block block, double maxDistance) {
    this.user = user;
    this.distanceSq = 0.25 + maxDistance * maxDistance;
    updateBlock(block);
  }

  @Override
  public boolean reselect(Block block) {
    if (this.block.equals(block)) {
      return false;
    }
    Vector3d newOrigin = block.center();
    if (user.eyeLocation().distanceSq(newOrigin) > distanceSq) {
      return false;
    }
    onDestroy();
    updateBlock(block);
    return true;
  }

  private void updateBlock(Block block) {
    this.block = block;
    this.origin = this.block.center();
    this.type = this.block.type();
  }

  @Override
  public void start(StateChain chain) {
    if (started) {
      return;
    }
    this.chain = chain;
    started = true;
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
    render();
    return UpdateResult.CONTINUE;
  }

  @Override
  public Block block() {
    return block;
  }

  protected void render() {
    Particle.SMOKE.builder(origin.add(0, 0.5, 0)).spawn(user.world());
  }
}
