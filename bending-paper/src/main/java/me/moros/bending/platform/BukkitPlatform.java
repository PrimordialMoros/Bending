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

package me.moros.bending.platform;

import java.util.Optional;

import me.moros.bending.Bending;
import me.moros.bending.gui.ElementMenu;
import me.moros.bending.model.ElementHandler;
import me.moros.bending.model.board.Board;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.platform.block.BlockInitializer;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.item.BukkitItemBuilder;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemBuilder;
import me.moros.bending.platform.item.ItemInitializer;
import me.moros.bending.platform.world.World;
import me.moros.math.Position;
import me.moros.math.bukkit.BukkitMathAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.ItemStack;

public class BukkitPlatform implements Platform, PlatformFactory {
  public BukkitPlatform(Bending plugin) {
    BukkitMathAdapter.register();
    String dir = plugin.getDataFolder().toString();
    new BlockInitializer(dir, plugin.logger());
    new ItemInitializer(dir, plugin.logger());
  }

  @Override
  public PlatformFactory factory() {
    return this;
  }

  @Override
  public PlatformType type() {
    return PlatformType.BUKKIT;
  }

  @Override
  public int currentTick() {
    return Bukkit.getCurrentTick();
  }

  @Override
  public Optional<Board> buildBoard(BendingPlayer player) {
    return Optional.of(new BoardImpl(player));
  }

  @Override
  public boolean buildMenu(ElementHandler handler, BendingPlayer player) {
    return ElementMenu.createMenu(handler, player);
  }

  @Override
  public ItemBuilder itemBuilder(Item item) {
    return new BukkitItemBuilder(item);
  }

  @Override
  public Entity createFallingBlock(World world, Position center, BlockState state, boolean gravity) {
    var w = PlatformAdapter.toBukkitWorld(world);
    var data = PlatformAdapter.toBukkitData(state);
    var bukkitEntity = w.spawnFallingBlock(center.to(Location.class, w), data);
    bukkitEntity.setGravity(gravity);
    bukkitEntity.setDropItem(false);
    return PlatformAdapter.fromBukkitEntity(bukkitEntity);
  }

  @Override
  public Entity createArmorStand(World world, Position center, Item type, boolean gravity) {
    var w = PlatformAdapter.toBukkitWorld(world);
    var item = new ItemStack(PlatformAdapter.ITEM_MATERIAL_INDEX.valueOrThrow(type));
    var bukkitEntity = w.spawn(center.to(Location.class, w), ArmorStand.class, as -> {
      as.setInvulnerable(true);
      as.setVisible(false);
      as.setGravity(gravity);
      as.getEquipment().setHelmet(item);
    });
    return PlatformAdapter.fromBukkitEntity(bukkitEntity);
  }
}
