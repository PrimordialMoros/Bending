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

package me.moros.bending.ability.common;

import java.util.HashMap;
import java.util.Map;

import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.ability.state.State;
import me.moros.bending.model.ability.state.StateChain;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.model.user.User;
import me.moros.bending.util.ParticleUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SelectedSource implements State {
  private static final Map<Block, SelectedSource> INSTANCES = new HashMap<>();

  private StateChain chain;
  private final User user;
  private Vector3d origin;
  private Block block;
  private Material material;
  private BlockState state;

  private final boolean particles;
  private final double distanceSq;

  private boolean started = false;
  private boolean forceRemove = false;

  public SelectedSource(@NonNull User user, @NonNull Block block, double maxDistance, @Nullable BlockData data) {
    this.user = user;
    this.distanceSq = 0.25 + maxDistance * maxDistance;
    particles = data == null;
    reselect(block, data);
  }

  public SelectedSource(@NonNull User user, @NonNull Block block, double maxDistance) {
    this(user, block, maxDistance, null);
  }

  public boolean reselect(@NonNull Block block) {
    return reselect(block, null);
  }

  public boolean reselect(@NonNull Block block, @Nullable BlockData data) {
    if (block.equals(this.block)) {
      return false;
    }
    Vector3d newOrigin = Vector3d.center(block);
    if (user.eyeLocation().distanceSq(newOrigin) > distanceSq) {
      return false;
    }
    onDestroy();
    this.block = block;
    this.origin = newOrigin;
    this.material = data == null ? block.getType() : data.getMaterial();
    if (data != null) {
      if (TempBlock.MANAGER.isTemp(block)) {
        state = block.getState();
      }
      TempBlock.create(block, data);
      INSTANCES.put(block, this);
    }
    return true;
  }

  @Override
  public void start(@NonNull StateChain chain) {
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
    if (block.getType() != material) {
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
  public @NonNull UpdateResult update() {
    if (!started || forceRemove) {
      return UpdateResult.REMOVE;
    }
    if (user.eyeLocation().distanceSq(origin) > distanceSq) {
      return UpdateResult.REMOVE;
    }
    Location loc = origin.toLocation(user.world());
    if (particles) {
      ParticleUtil.of(Particle.SMOKE_NORMAL, loc.add(0, 0.5, 0)).spawn();
    }
    return UpdateResult.CONTINUE;
  }

  public @NonNull Block selectedSource() {
    return block;
  }

  public void onDestroy() {
    if (!particles && block != null && block.getType() == material) {
      if (state != null) {
        state.update(true, false);
      } else {
        TempBlock.MANAGER.get(block).ifPresent(TempBlock::revert);
      }
    }
    INSTANCES.remove(block);
  }

  public static void tryRevertSource(@NonNull Block block) {
    SelectedSource selectedSource = INSTANCES.get(block);
    if (selectedSource != null) {
      selectedSource.onDestroy();
    }
  }
}
