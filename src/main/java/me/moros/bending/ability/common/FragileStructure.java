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

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

import me.moros.bending.Bending;
import me.moros.bending.game.temporal.TempBlock;
import me.moros.bending.util.Metadata;
import me.moros.bending.util.ParticleUtil;
import me.moros.bending.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.checkerframework.checker.nullness.qual.NonNull;

public class FragileStructure {
  private final Collection<Block> fragileBlocks;
  private final Predicate<Block> predicate;
  private int health;

  private FragileStructure(Collection<Block> fragileBlocks, int health, Predicate<Block> predicate) {
    this.fragileBlocks = Set.copyOf(fragileBlocks);
    this.health = health;
    this.predicate = predicate;
    this.fragileBlocks.forEach(b -> b.setMetadata(Metadata.DESTRUCTIBLE, Metadata.customMetadata(this)));
  }

  public int health() {
    return health;
  }

  /**
   * @return unmodifiable collection of blocks belonging to the same fragile structure
   */
  public @NonNull Collection<@NonNull Block> fragileBlocks() {
    return fragileBlocks;
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

  public static Optional<FragileStructure> create(@NonNull Collection<@NonNull Block> blocks, int health, @NonNull Predicate<Block> predicate) {
    if (health < 0 || blocks.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new FragileStructure(blocks, health, predicate));
  }

  public static boolean tryDamageStructure(@NonNull Collection<@NonNull Block> blocks, int damage) {
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
      if (!data.predicate.test(block)) {
        continue;
      }
      BlockData blockData = block.getType().createBlockData();
      TempBlock.createAir(block);
      block.removeMetadata(Metadata.DESTRUCTIBLE, Bending.plugin());
      Location center = block.getLocation().add(0.5, 0.5, 0.5);
      ParticleUtil.create(Particle.BLOCK_CRACK, center).count(2)
        .offset(0.3, 0.3, 0.3).data(blockData).spawn();
      if (ThreadLocalRandom.current().nextInt(3) == 0) {
        SoundUtil.playSound(center, blockData.getSoundGroup().getBreakSound(), 2, 1);
      }
    }
  }
}
