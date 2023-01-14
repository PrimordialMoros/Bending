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

import java.nio.file.Path;
import java.util.Optional;

import me.moros.bending.SpongeBending;
import me.moros.bending.model.ElementHandler;
import me.moros.bending.model.board.Board;
import me.moros.bending.model.user.BendingPlayer;
import me.moros.bending.platform.block.BlockInitializer;
import me.moros.bending.platform.block.BlockState;
import me.moros.bending.platform.entity.Entity;
import me.moros.bending.platform.item.Item;
import me.moros.bending.platform.item.ItemBuilder;
import me.moros.bending.platform.item.ItemInitializer;
import me.moros.bending.platform.item.ItemSnapshot;
import me.moros.bending.platform.item.SpongeItemBuilder;
import me.moros.bending.platform.world.World;
import me.moros.math.Position;
import me.moros.math.sponge.SpongeMathAdapter;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.equipment.EquipmentTypes;

public class SpongePlatform implements Platform, PlatformFactory {
  public SpongePlatform(Path path, SpongeBending plugin) {
    SpongeMathAdapter.register();
    new BlockInitializer(path, plugin.logger());
    new ItemInitializer(path, plugin.logger());
  }

  @Override
  public PlatformFactory factory() {
    return this;
  }

  @Override
  public PlatformType type() {
    return PlatformType.SPONGE;
  }

  @Override
  public int currentTick() {
    return (int) Sponge.server().runningTimeTicks().ticks();
  }

  @Override
  public Optional<Board> buildBoard(BendingPlayer player) {
    return Optional.of(new BoardImpl(player));
  }

  @Override
  public boolean buildMenu(ElementHandler handler, BendingPlayer player) {
    //return ElementMenu.createMenu(handler, player); // TODO add gui
    return false;
  }

  @Override
  public ItemBuilder itemBuilder(Item item) {
    return new SpongeItemBuilder(ItemStack.of(PlatformAdapter.ITEM_MATERIAL_INDEX.keyOrThrow(item)));
  }

  @Override
  public ItemBuilder itemBuilder(ItemSnapshot snapshot) {
    return new SpongeItemBuilder(PlatformAdapter.toSpongeItem(snapshot));
  }

  @Override
  public Entity createFallingBlock(World world, Position center, BlockState state, boolean gravity) {
    var w = PlatformAdapter.toSpongeWorld(world);
    var spongeEntity = w.createEntity(EntityTypes.FALLING_BLOCK, center.to(org.spongepowered.math.vector.Vector3d.class));
    spongeEntity.blockState().set(PlatformAdapter.toSpongeData(state));
    spongeEntity.gravityAffected().set(gravity);
    spongeEntity.dropAsItem().set(false);
    w.spawnEntity(spongeEntity);
    return PlatformAdapter.fromSpongeEntity(spongeEntity);
  }

  @Override
  public Entity createArmorStand(World world, Position center, Item type, boolean gravity) {
    var w = PlatformAdapter.toSpongeWorld(world);
    var item = ItemStack.of(PlatformAdapter.ITEM_MATERIAL_INDEX.keyOrThrow(type));
    var spongeEntity = w.createEntity(EntityTypes.ARMOR_STAND, center.to(org.spongepowered.math.vector.Vector3d.class));
    spongeEntity.invulnerable().set(true);
    spongeEntity.invisible().set(true);
    spongeEntity.gravityAffected().set(gravity);
    spongeEntity.equip(EquipmentTypes.HEAD, item);
    w.spawnEntity(spongeEntity);
    return PlatformAdapter.fromSpongeEntity(spongeEntity);
  }
}
