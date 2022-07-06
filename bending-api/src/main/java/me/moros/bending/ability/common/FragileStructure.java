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

package me.moros.bending.ability.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.model.math.Vector3d;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import me.moros.bending.util.metadata.Metadata;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class FragileStructure implements Iterable<Block> {
  private final Collection<Block> fragileBlocks;
  private final Predicate<Block> predicate;
  private int health;

  private FragileStructure(Collection<Block> fragileBlocks, Predicate<Block> predicate, int health) {
    this.fragileBlocks = fragileBlocks;
    this.predicate = predicate;
    this.health = health;
    this.fragileBlocks.forEach(b -> b.setMetadata(Metadata.DESTRUCTIBLE, Metadata.of(this)));
  }

  public int health() {
    return health;
  }

  /**
   * Try to subtract the specified amount of damage from this structure's health.
   * If health drops at zero or below then the structure will shatter.
   * Note: Provide a non positive damage value to instantly destroy the structure.
   * @param damage the amount of damage to inflict
   * @return the remaining structure health
   */
  private int damageStructure(int damage) {
    if (damage > 0 && health > damage) {
      health -= damage;
      return health;
    }
    destroyStructure(this);
    return 0;
  }

  public static boolean tryDamageStructure(@NonNull Iterable<@NonNull Block> blocks, int damage) {
    for (Block block : blocks) {
      if (block.hasMetadata(Metadata.DESTRUCTIBLE)) {
        FragileStructure structure = (FragileStructure) block.getMetadata(Metadata.DESTRUCTIBLE).get(0).value();
        if (structure != null) {
          structure.damageStructure(damage);
          return true;
        }
      }
    }
    return false;
  }

  public static void destroyStructure(@NonNull FragileStructure data) {
    for (Block block : data.fragileBlocks) {
      Metadata.remove(block, Metadata.DESTRUCTIBLE);
      if (!data.predicate.test(block)) {
        continue;
      }
      BlockData blockData = block.getType().createBlockData();
      TempBlock.air().build(block);
      Vector3d center = Vector3d.center(block);
      ParticleUtil.of(Particle.BLOCK_CRACK, center).count(2).offset(0.3).data(blockData).spawn(block.getWorld());
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.of(blockData.getSoundGroup().getBreakSound(), 2, 1).play(block);
      }
    }
  }

  public static @NonNull Builder builder() {
    return new Builder();
  }

  @Override
  public @NonNull Iterator<Block> iterator() {
    return fragileBlocks.iterator();
  }

  public static final class Builder {
    private Predicate<Block> predicate;
    private int health = 10;

    private Builder() {
    }

    public @NonNull Builder health(int health) {
      this.health = Math.max(1, health);
      return this;
    }

    public @NonNull Builder predicate(@NonNull Predicate<Block> predicate) {
      this.predicate = Objects.requireNonNull(predicate);
      return this;
    }

    public @Nullable FragileStructure build(@NonNull Collection<@NonNull Block> blocks) {
      if (blocks.isEmpty()) {
        return null;
      }
      return new FragileStructure(Set.copyOf(blocks), predicate, health);
    }
  }
}
