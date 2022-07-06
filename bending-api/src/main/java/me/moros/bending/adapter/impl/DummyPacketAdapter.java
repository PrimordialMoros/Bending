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

package me.moros.bending.adapter.impl;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import me.moros.bending.adapter.PacketAdapter;
import me.moros.bending.model.math.Vector3d;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.util.Ticks;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class DummyPacketAdapter implements PacketAdapter {
  DummyPacketAdapter() {
  }

  @Override
  public void sendNotification(@NonNull Player player, @NonNull Material material, @NonNull Component title) {
    Times times = Times.times(Ticks.duration(10), Ticks.duration(70), Ticks.duration(10));
    player.showTitle(Title.title(title, Component.empty(), times));
  }

  @Override
  public int createArmorStand(@NonNull World world, @NonNull Vector3d center, @NonNull Material material, @NonNull Vector3d velocity, boolean gravity) {
    return 0;
  }

  @Override
  public int createFallingBlock(@NonNull World world, @NonNull Vector3d center, @NonNull BlockData data, @NonNull Vector3d velocity, boolean gravity) {
    return 0;
  }

  @Override
  public void fakeBlock(@NonNull World world, @NonNull Vector3d center, @NonNull BlockData data) {
  }

  @Override
  public void fakeBreak(@NonNull World world, @NonNull Vector3d center, byte progress) {
  }

  @Override
  public void refreshBlocks(@NonNull Collection<@NonNull Block> blocks, @NonNull World world, @NonNull Vector3d center) {
    if (blocks.isEmpty()) {
      return;
    }
    broadcast(refreshBlocks(blocks), world, center, (world.getViewDistance() + 1) << 4);
  }

  @Override
  public void destroy(int[] ids) {
  }

  private Consumer<Player> refreshBlocks(Collection<Block> blocks) {
    int size = blocks.size();
    if (size == 1) {
      var b = blocks.iterator().next();
      return p -> p.sendBlockChange(b.getLocation(), b.getBlockData());
    } else {
      var map = blocks.stream().collect(Collectors.toMap(Block::getLocation, Block::getBlockData));
      return p -> p.sendMultiBlockChange(map, true);

    }
  }

  private void broadcast(Consumer<Player> consumer, World world, Vector3d center) {
    broadcast(consumer, world, center, world.getViewDistance() << 4);
  }

  private void broadcast(Consumer<Player> consumer, World world, Vector3d center, int dist) {
    int distanceSq = dist * dist;
    for (var player : world.getPlayers()) {
      if (new Vector3d(player.getLocation()).distanceSq(center) <= distanceSq) {
        consumer.accept(player);
      }
    }
  }
}
