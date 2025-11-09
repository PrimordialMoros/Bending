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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.api.collision.geometry.Ray;
import me.moros.bending.api.platform.block.Block;
import me.moros.bending.api.platform.block.BlockType;
import me.moros.bending.api.temporal.TempBlock;
import me.moros.bending.api.temporal.TempEntity;
import me.moros.bending.api.util.KeyUtil;
import me.moros.bending.api.util.data.DataKey;
import me.moros.math.Vector3d;
import me.moros.math.VectorUtil;
import org.jspecify.annotations.Nullable;

public class FragileStructure implements Iterable<Block> {
  public static final DataKey<FragileStructure> DESTRUCTIBLE = KeyUtil.data("destructible", FragileStructure.class);

  private final Collection<Block> fragileBlocks;
  private final Predicate<Block> predicate;
  private final boolean fallingBlocks;
  private int health;

  protected <T extends FragileStructure> FragileStructure(Builder<T> builder) {
    this.fragileBlocks = Set.copyOf(builder.blocks);
    this.predicate = builder.predicate;
    this.fallingBlocks = builder.fallingBlocks;
    this.health = builder.health;
    this.fragileBlocks.forEach(b -> b.add(DESTRUCTIBLE, this));
  }

  public int health() {
    return health;
  }

  private int damageStructure(int damage, Ray ray) {
    if (damage > 0 && health > damage) {
      health -= damage;
      return health;
    }
    destroyStructure(ray);
    return 0;
  }

  private void destroyStructure(Ray ray) {
    for (Block block : fragileBlocks) {
      block.remove(DESTRUCTIBLE);
      if (!predicate.test(block)) {
        continue;
      }
      onDestroy(block, ray);
    }
  }

  protected void onDestroy(Block block, Ray ray) {
    BlockType type = block.type();
    TempBlock.air().build(block);
    type.asParticle(block.center()).count(2).offset(0.3).spawn(block.world());
    if (ThreadLocalRandom.current().nextInt(3) == 0) {
      type.soundGroup().breakSound().asEffect(2, 1).play(block);
    }
    if (fallingBlocks) {
      Vector3d dir = ray.position().add(ray.direction().normalize().multiply(8)).subtract(block.center());
      Vector3d velocity = VectorUtil.gaussianOffset(dir.normalize().multiply(0.3), 0.05);
      TempEntity.fallingBlock(type.defaultState()).velocity(velocity).duration(3000).build(block);
    }
  }

  public static boolean tryDamageStructure(Block block, int damage, Ray ray) {
    return tryDamageStructure(List.of(block), damage, ray);
  }

  public static boolean tryDamageStructure(Iterable<Block> blocks, int damage, Ray ray) {
    for (Block block : blocks) {
      FragileStructure structure = block.get(DESTRUCTIBLE).orElse(null);
      if (structure != null) {
        structure.damageStructure(damage, ray);
        return true;
      }
    }
    return false;
  }

  @Override
  public Iterator<Block> iterator() {
    return fragileBlocks.iterator();
  }

  public static Builder<FragileStructure> builder() {
    return builder(FragileStructure::new);
  }

  public static <T extends FragileStructure> Builder<T> builder(Function<Builder<T>, T> constructor) {
    return new Builder<>(constructor);
  }

  public static final class Builder<T extends FragileStructure> {
    private final Function<Builder<T>, T> constructor;
    private final Collection<Block> blocks = new ArrayList<>();
    private Predicate<Block> predicate;
    private boolean fallingBlocks;
    private int health = 10;

    private Builder(Function<Builder<T>, T> constructor) {
      this.constructor = constructor;
    }

    public Builder<T> fallingBlocks(boolean fallingBlocks) {
      this.fallingBlocks = fallingBlocks;
      return this;
    }

    public Builder<T> health(int health) {
      this.health = Math.max(1, health);
      return this;
    }

    public Builder<T> predicate(Predicate<Block> predicate) {
      this.predicate = Objects.requireNonNull(predicate);
      return this;
    }

    public Builder<T> add(Collection<Block> blocks) {
      this.blocks.addAll(List.copyOf(blocks));
      return this;
    }

    public @Nullable FragileStructure build() {
      if (blocks.isEmpty() || predicate == null) {
        return null;
      }
      return constructor.apply(this);
    }
  }
}
