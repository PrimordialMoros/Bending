/*
 * Copyright 2020-2024 Moros
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

package me.moros.bending.common.ability.earth.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import me.moros.bending.api.ability.common.FragileStructure;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockState;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.user.User;
import me.moros.bending.api.util.material.EarthMaterials;
import me.moros.bending.api.util.material.MaterialUtil;
import me.moros.math.Position;
import me.moros.math.Vector3d;
import me.moros.math.Vector3i;

public final class Boulder {
  private final Map<Vector3i, BlockState> data;
  private final AABB bounds;
  private final AABB preciseBounds;
  private final World world;
  private User user;
  private Vector3d center;

  private final int size;
  private final long expireTime;

  public Boulder(User user, Block centerBlock, int size, long duration) {
    this.user = user;
    this.world = user.world();
    this.size = size;
    expireTime = System.currentTimeMillis() + duration;
    data = new HashMap<>();
    center = centerBlock.center();
    double hr = size / 2.0;
    preciseBounds = AABB.of(Vector3d.of(-hr, -hr, -hr), Vector3d.of(hr, hr, hr));
    bounds = preciseBounds.grow(Vector3d.ONE);
    initData(centerBlock);
  }

  private void initData(Block centerBlock) {
    int half = (size - 1) / 2;
    Block temp = centerBlock.offset(Direction.DOWN, half);
    List<BlockType> earthData = new ArrayList<>();
    for (int dy = -half; dy <= half; dy++) {
      for (int dz = -half; dz <= half; dz++) {
        for (int dx = -half; dx <= half; dx++) {
          Block block = temp.offset(dx, dy, dz);
          if (!user.canBuild(block)) {
            continue;
          }
          BlockState bd = null;
          if (EarthMaterials.isEarthNotLava(user, block)) {
            bd = MaterialUtil.solidType(block.type()).defaultState();
            earthData.add(bd.type());
          } else if (MaterialUtil.isTransparent(block)) {
            if (earthData.isEmpty()) {
              bd = BlockType.DIRT.defaultState();
            } else {
              bd = earthData.get(ThreadLocalRandom.current().nextInt(earthData.size())).defaultState();
            }
          }
          if (bd != null && (Math.abs(dx) + Math.abs(dy) + Math.abs(dz)) % 2 == 0) {
            data.put(Vector3i.of(dx, dy, dz), bd);
          }
        }
      }
    }
  }

  public User user() {
    return user;
  }

  public void user(User user) {
    this.user = user;
  }

  public World world() {
    return world;
  }

  public AABB bounds() {
    return bounds;
  }

  public AABB preciseBounds() {
    return preciseBounds;
  }

  public int size() {
    return size;
  }

  public long expireTime() {
    return expireTime;
  }

  public boolean isEmpty() {
    return data.isEmpty();
  }

  public int dataSize() {
    return data.size();
  }

  public boolean isValidBlock(Block block) {
    if (!MaterialUtil.isTransparent(block) || !TempBlock.isBendable(block)) {
      return false;
    }
    return user.canBuild(block);
  }

  public void updateData() {
    data.entrySet().removeIf(entry -> {
      BlockType type = world.getBlockType(center.add(entry.getKey()));
      return type != entry.getValue().type();
    });
  }

  public void updateData(BiFunction<Vector3i, BlockState, BlockState> mapper) {
    data.replaceAll(mapper);
  }

  public void clearData() {
    data.clear();
  }

  public boolean blendSmash(Vector3d direction) {
    int originalSize = data.size();
    Collection<Block> removed = new ArrayList<>();
    Iterator<Vector3i> iterator = data.keySet().iterator();
    while (iterator.hasNext()) {
      Block block = world.blockAt(center.add(iterator.next()));
      if (!isValidBlock(block)) {
        removed.add(block);
        iterator.remove();
      }
    }
    FragileStructure.tryDamageStructure(removed, 4 * removed.size(), Ray.of(center, direction));
    return !data.isEmpty() && originalSize - data.size() <= size;
  }

  public boolean isValidCenter(Block check) {
    Vector3d temp = check.center();
    return data.keySet().stream().map(v -> world.blockAt(temp.add(v))).allMatch(this::isValidBlock);
  }

  public Vector3d center() {
    return center;
  }

  public void center(Position position) {
    this.center = position.center();
  }

  public Collider collider() {
    return bounds.at(center);
  }

  public Map<Block, BlockState> data() {
    return data.entrySet().stream()
      .collect(Collectors.toMap(e -> world.blockAt(center.add(e.getKey())), Entry::getValue));
  }

}
