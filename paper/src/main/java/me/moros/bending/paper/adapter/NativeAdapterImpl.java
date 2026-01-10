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

package me.moros.bending.paper.adapter;

import io.papermc.paper.adventure.PaperAdventure;
import me.moros.bending.api.platform.item.Item;
import me.moros.bending.api.platform.world.World;
import me.moros.bending.common.adapter.AbstractNativeAdapter;
import me.moros.bending.paper.platform.PlatformAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftEntity;

final class NativeAdapterImpl extends AbstractNativeAdapter {
  NativeAdapterImpl() {
    super(MinecraftServer.getServer().getPlayerList());
  }

  @Override
  protected ServerLevel adapt(World world) {
    return ((CraftWorld) PlatformAdapter.toBukkitWorld(world)).getHandle();
  }

  @Override
  protected BlockState adapt(me.moros.bending.api.platform.block.BlockState state) {
    return ((CraftBlockData) PlatformAdapter.toBukkitData(state)).getState();
  }

  @Override
  protected Entity adapt(me.moros.bending.api.platform.entity.Entity entity) {
    return ((CraftEntity) PlatformAdapter.toBukkitEntity(entity)).getHandle();
  }

  @Override
  protected ItemStack adapt(Item item) {
    var id = Identifier.fromNamespaceAndPath(item.key().namespace(), item.key().value());
    return BuiltInRegistries.ITEM.getValue(id).getDefaultInstance();
  }

  @Override
  protected Component adapt(net.kyori.adventure.text.Component component) {
    return PaperAdventure.asVanilla(component);
  }

  @Override
  protected int nextEntityId() {
    return net.minecraft.world.entity.Entity.nextEntityId();
  }
}
