/*
 * Copyright 2020-2026 Moros
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

package me.moros.bending.common.ability.fire;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import me.moros.bending.api.ability.AbilityDescription;
import me.moros.bending.api.ability.AbilityInstance;
import me.moros.bending.api.ability.Activation;
import me.moros.bending.api.collision.Collision;
import me.moros.bending.api.collision.geometry.AABB;
import me.moros.bending.api.collision.geometry.Collider;
import me.moros.bending.api.platform.Direction;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.platform.particle.Particle;
import me.moros.bending.api.platform.sound.SoundEffect;
import me.moros.bending.api.platform.world.WorldUtil;
import me.moros.bending.api.registry.Registries;
import me.moros.bending.api.user.User;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;

public class FirePropagate extends AbilityInstance {
  private static final int CELL_HEALTH = 10;
  private static final int CELL_ENERGY = 8;

  private static AbilityDescription ABILITY_DESC;

  private final Map<Block, Cell> cells = new HashMap<>();
  private int spreadingEnergy = 0;

  public FirePropagate(AbilityDescription desc) {
    super(desc);
  }

  @Override
  public boolean activate(User user, Activation method) {
    return false;
  }

  private boolean activate(User user, Block origin, int spreadingEnergy) {
    this.user = user;
    if (!canPropagate(origin.type())) {
      return false;
    }
    cells.compute(origin, (_, c) -> {
      if (c == null) {
        c = new Cell(CELL_HEALTH, CELL_ENERGY);
      }
      c.ignite();
      return c;
    });
    this.spreadingEnergy += spreadingEnergy;
    return true;
  }

  @Override
  public void loadConfig() {
  }

  @Override
  public UpdateResult update() {
    Set<Block> toRemove = new HashSet<>();
    Set<Block> burning = new HashSet<>();

    // Pass 1, tick and create new cells
    for (var entry : List.copyOf(cells.entrySet())) {
      Block block = entry.getKey();
      if (!canPropagate(block.type())) {
        toRemove.add(block);
        continue;
      }
      Cell cell = entry.getValue();
      switch (cell.updateState()) {
        case DAMAGE_SELF -> onDamage(block);
        case BURN -> {
          burning.add(block);
          createNeighbourCells(block);
        }
        case REMOVE -> toRemove.add(block);
      }
    }

    // Pass 2, aggregate cell damage and apply it
    Map<Block, CellDamage> pendingDamage = new HashMap<>();
    for (Block from : burning) {
      onBurn(from);
      for (Direction dir : WorldUtil.FACES) {
        Block to = from.offset(dir);
        Cell cell = cells.get(to);
        if (cell != null) {
          pendingDamage.compute(to, (_, cd) -> {
            if (cd == null) {
              return new CellDamage(cell);
            }
            return cd.increment();
          });
        }
      }
    }
    pendingDamage.values().forEach(CellDamage::applyDamage);

    for (Block b : toRemove) {
      cells.remove(b);
      onRemove(b);
    }
    return cells.isEmpty() ? UpdateResult.REMOVE : UpdateResult.CONTINUE;
  }

  @Override
  public Collection<Collider> colliders() {
    if (cells.isEmpty()) {
      return List.of();
    }
    return cells.keySet().stream().<Collider>map(AABB.BLOCK_BOUNDS::at).toList();
  }

  @Override
  public void onCollision(Collision collision) {
    super.onCollision(collision);
  }

  @Override
  public void onDestroy() {
    cells.clear();
  }

  private void createNeighbourCells(Block from) {
    for (Direction dir : WorldUtil.FACES) {
      Block to = from.offset(dir);
      if (!canPropagate(to.type())) {
        continue;
      }
      if (!cells.containsKey(to)) {
        if (spreadingEnergy >= CELL_HEALTH) {
          spreadingEnergy -= CELL_HEALTH;
          cells.put(to, new Cell(CELL_HEALTH, CELL_ENERGY));
          onSpread(from, to);
        }
      }
    }
  }

  private void onDamage(Block block) {
    if (ThreadLocalRandom.current().nextInt(3) == 0) {
      Particle.SMALL_FLAME.builder(block.center()).count(4).offset(0.4).spawn(block.world());
    }
    if (ThreadLocalRandom.current().nextInt(4) == 0) {
      Particle.SMOKE.builder(block.center()).count(2).offset(0.4).spawn(block.world());
    }
  }

  private void onBurn(Block block) {
    Particle.FLAME.builder(block.center()).offset(0.2).spawn(block.world());
  }

  private void onSpread(Block from, Block to) {
    for (int i = 0; i < 4; i++) {
      Vector3d pos = VectorUtil.gaussianOffset(from.center(), 0.25);
      Vector3d dir = to.center().subtract(pos).normalize();
      Particle.FLAME.builder(pos).count(0).offset(dir).extra(0.04).spawn(from.world());
    }
    if (ThreadLocalRandom.current().nextInt(7) == 0) {
      SoundEffect.FIRE.play(to);
    }
  }

  private void onRemove(Block block) {
    if (user.canBuild(block)) {
      block.setType(BlockType.AIR);
    }
  }

  public static boolean create(User user, Block origin, int spreadingEnergy) {
    if (ABILITY_DESC == null) {
      ABILITY_DESC = Objects.requireNonNull(Registries.ABILITIES.fromString("FirePropagate"));
    }
    if (spreadingEnergy <= 0) {
      return false;
    }
    FirePropagate instance = user.game().abilityManager(user.worldKey())
      .firstInstance(user, FirePropagate.class).orElse(null);
    if (instance != null) {
      return instance.activate(user, origin, spreadingEnergy * CELL_HEALTH);
    } else {
      instance = new FirePropagate(ABILITY_DESC);
      if (instance.activate(user, origin, spreadingEnergy * CELL_HEALTH)) {
        user.game().abilityManager(user.worldKey()).addAbility(instance);
        return true;
      }
    }
    return false;
  }

  public static boolean canPropagate(BlockType type) {
    return type == BlockType.COBWEB || type == BlockType.TRIPWIRE;
  }

  private static final class Cell {
    private int igniteEnergy;
    private int burnEnergy;

    private Cell(int igniteEnergy, int burnEnergy) {
      this.igniteEnergy = igniteEnergy;
      this.burnEnergy = burnEnergy;
    }

    private void ignite() {
      this.igniteEnergy = 0;
    }

    private void burn(int amount) {
      int consumedEnergy = Math.min(amount, igniteEnergy);
      igniteEnergy -= consumedEnergy;
      burnEnergy -= (amount - consumedEnergy);
    }

    private State updateState() {
      burn(1);
      if (igniteEnergy > 0) {
        return State.DAMAGE_SELF;
      } else if (burnEnergy > 0) {
        return State.BURN;
      } else {
        return State.REMOVE;
      }
    }
  }

  private static final class CellDamage {
    private final Cell cell;
    private int damage;

    private CellDamage(Cell cell) {
      this.cell = cell;
      this.damage = 1;
    }

    private CellDamage increment() {
      this.damage++;
      return this;
    }

    private void applyDamage() {
      cell.burn(damage);
    }
  }

  private enum State {DAMAGE_SELF, BURN, REMOVE}
}
