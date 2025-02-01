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

package me.moros.bending.common.ability;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.Updatable;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.math.FastMath;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;

// TODO wind dmg multiplier
public class FirePropagate implements Updatable {
  private static final int CELL_HEALTH = 10;
  private static final int CELL_ENERGY = 10;

  private final Map<Block, FireCellData> cells;
  private final Set<Block> visited;
  private int spreadingEnergy;

  private FirePropagate(int spreadingEnergy) {
    this.cells = new HashMap<>();
    this.visited = new HashSet<>();
    this.spreadingEnergy = FastMath.ceil(0.75 * CELL_ENERGY * spreadingEnergy);
  }

  public boolean ignite(List<Block> blocks) {
    blocks.forEach(b -> trySpread(b, b, cells));
    return !cells.isEmpty();
  }

  @Override
  public UpdateResult update() {
    Iterator<Entry<Block, FireCellData>> iterator = cells.entrySet().iterator();
    Map<Block, FireCellData> pending = new HashMap<>();
    while (iterator.hasNext()) {
      Entry<Block, FireCellData> entry = iterator.next();
      Block current = entry.getKey();
      switch (entry.getValue().updateState()) {
        case DAMAGE_SELF -> onDamage(current);
        case SPREAD_TO_NEIGHBOURS -> {
          for (Direction dir : WorldUtil.FACES) {
            trySpread(current, current.offset(dir), pending);
          }
        }
        case BURN -> onBurn(current);
        case REMOVE -> {
          iterator.remove();
          onRemove(current);
        }
      }
    }
    cells.putAll(pending);
    return cells.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  private void trySpread(Block from, Block to, Map<Block, FireCellData> map) {
    if (visited.add(to)) {
      if (canSpread(to)) {
        map.put(to, new FireCellData(CELL_HEALTH, CELL_ENERGY));
        onSpread(from, to);
        spreadingEnergy -= ThreadLocalRandom.current().nextInt(1, CELL_ENERGY);
      }
    }
  }

  protected void onDamage(Block block) {
    if (ThreadLocalRandom.current().nextInt(3) == 0) {
      Particle.SMALL_FLAME.builder(block.center()).count(4).offset(0.4).build();
    }
    if (ThreadLocalRandom.current().nextInt(4) == 0) {
      Particle.SMOKE.builder(block.center()).count(2).offset(0.4).build();
    }
  }

  protected boolean canSpread(Block block) {
    return spreadingEnergy > 0 && canPropagate(block.type());
  }

  protected void onSpread(Block from, Block to) {
    for (int i = 0; i < 4; i++) {
      Vector3d pos = VectorUtil.gaussianOffset(from.center(), 0.25);
      Vector3d dir = to.center().subtract(pos).normalize();
      Particle.FLAME.builder(pos).count(0).offset(dir).extra(0.04).spawn(from.world());
    }
    if (ThreadLocalRandom.current().nextInt(4) == 0) {
      SoundEffect.FIRE.play(to);
    }
  }

  protected void onBurn(Block block) {
    Particle.FLAME.builder(block.center()).offset(0.2).spawn(block.world());
  }

  protected void onRemove(Block block) {
    TempBlock.air().build(block);
  }

  public static Optional<FirePropagate> create(int spreadingEnergy, List<Block> blocks) {
    FirePropagate instance = new FirePropagate(spreadingEnergy);
    return instance.ignite(blocks) ? Optional.of(instance) : Optional.empty();
  }

  public static boolean canPropagate(BlockType type) {
    return type == BlockType.COBWEB;
  }

  private static final class FireCellData {
    private int health;
    private int energy;

    private FireCellData(int maxHealth, int energy) {
      this.health = maxHealth;
      this.energy = energy;
    }

    private State updateState() {
      if (health > 0) {
        health--;
        if (health <= 0) {
          return State.SPREAD_TO_NEIGHBOURS;
        }
        return State.DAMAGE_SELF;
      }
      if (energy > 0) {
        energy--;
        return State.BURN;
      }
      return State.REMOVE;
    }
  }

  private enum State {DAMAGE_SELF, SPREAD_TO_NEIGHBOURS, BURN, REMOVE}
}
