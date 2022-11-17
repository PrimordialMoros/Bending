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

package me.moros.bending.model.ability.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;

import me.moros.bending.model.collision.geometry.Ray;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.temporal.TempBlock;
import me.moros.bending.temporal.TempEntity;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.VectorUtil;
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.Nullable;

public class FragileStructure implements Iterable<Block> {
  private final Collection<Block> fragileBlocks;
  private final Predicate<Block> predicate;
  private final boolean fallingBlocks;
  private int health;

  protected <T extends FragileStructure> FragileStructure(Builder<T> builder) {
    this.fragileBlocks = Set.copyOf(builder.blocks);
    this.predicate = builder.predicate;
    this.fallingBlocks = builder.fallingBlocks;
    this.health = builder.health;
    this.fragileBlocks.forEach(b -> Metadata.add(b, Metadata.DESTRUCTIBLE, this));
  }

  public int health() {
    return health;
  }

  /**
   * Try to subtract the specified amount of damage from this structure's health.
   * If health drops at zero or below then the structure will shatter.
   * <p>Note: Provide a non-positive damage value to instantly destroy the structure.
   * @param damage the amount of damage to inflict
   * @return the remaining structure health
   */
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
      Metadata.remove(block, Metadata.DESTRUCTIBLE);
      if (!predicate.test(block)) {
        continue;
      }
      onDestroy(block, ray);
    }
  }

  protected void onDestroy(Block block, Ray ray) {
    BlockData blockData = block.getType().createBlockData();
    TempBlock.air().build(block);
    Vector3d center = Vector3d.center(block);
    ParticleUtil.of(Particle.BLOCK_CRACK, center).count(2).offset(0.3).data(blockData).spawn(block.getWorld());
    if (ThreadLocalRandom.current().nextInt(3) == 0) {
      SoundUtil.of(blockData.getSoundGroup().getBreakSound(), 2, 1).play(block);
    }
    if (fallingBlocks) {
      Vector3d dir = ray.origin.add(ray.direction.normalize().multiply(8)).subtract(Vector3d.center(block));
      Vector3d velocity = VectorUtil.gaussianOffset(dir.normalize().multiply(0.3), 0.05);
      TempEntity.builder(blockData).velocity(velocity).duration(5000).build(block);
    }
  }

  public static boolean tryDamageStructure(Block block, int damage, Ray ray) {
    return tryDamageStructure(List.of(block), damage, ray);
  }

  public static boolean tryDamageStructure(Iterable<Block> blocks, int damage, Ray ray) {
    for (Block block : blocks) {
      if (block.hasMetadata(Metadata.DESTRUCTIBLE)) {
        FragileStructure structure = (FragileStructure) block.getMetadata(Metadata.DESTRUCTIBLE).get(0).value();
        if (structure != null) {
          structure.damageStructure(damage, ray);
          return true;
        }
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
